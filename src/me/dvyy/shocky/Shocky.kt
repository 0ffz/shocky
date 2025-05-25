package me.dvyy.shocky

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.html.dom.append
import kotlinx.html.dom.write
import kotlinx.html.script
import me.dvyy.shocky.dev.autoReloadScript
import me.dvyy.shocky.dev.installTailwindIfNecessary
import org.slf4j.helpers.NOPLogger
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

class Shocky(
    val dest: Path,
    val routing: SiteRouting,
    val assets: List<Path>,
    val useTailwind: Boolean,
    val tailwindVersion: String,
    val port: Int,
    val beforeGenerate: () -> Unit,
    val afterGenerate: () -> Unit,
    val watch: List<Path> = listOf(),
) {
    val generatorFlow = MutableSharedFlow<Unit>()

    @OptIn(ExperimentalPathApi::class)
    suspend fun generate(devMode: Boolean) = withContext(Dispatchers.IO) {
        measureTime {
            if (!devMode) dest.deleteRecursively()
            dest.createDirectories()
        }.let { println("Cleared output in: $it") }

        launch {
            beforeGenerate()
            measureTime {
                assets.forEach { it.copyToRecursively(dest / it.name, followLinks = false, overwrite = true) }
            }.let { println("Copied extra inputs in: $it") }
            println("Generated html files in: ${measureTime { generateDocuments(devMode) }}")

            if (useTailwind) {
                val tailwindPath = dest / "../build/tailwind"
                installTailwindIfNecessary(tailwindPath, tailwindVersion)
                runCommand(tailwindPath.pathString, "-o", "out/assets/tailwind/styles.css", "--minify")
            }

            afterGenerate()
        }
    }

    suspend fun generateDocuments(devMode: Boolean) = withContext(Dispatchers.IO) {
        routing.assets.forEach {
            val dest = dest / it.relativeTo(routing.route)
            dest.createParentDirectories()
            it.copyTo(dest, overwrite = true)
        }
        routing.documents.map { document ->
            launch {
                val path = dest / document.path.relativeTo(routing.route)
                path.createParentDirectories().also { if (it.notExists()) it.createFile() }
                    .writer()
                    .use { writer ->
                        document.page.html
                            ?.apply {
                                if (devMode) getElementsByTagName("head").item(0)?.append {
                                    script(src = "/assets/scripts/autoreload.js") {
                                        defer = true
                                    }
                                }
                                val nodes = getElementsByTagName("img")
                                for (i in 0 until nodes.length) {
                                    val href = nodes.item(i).attributes.getNamedItem("src")
                                    val value = href.nodeValue
                                    if (!value.startsWith("/") && !value.startsWith("http"))
                                        href.nodeValue =
                                            "/" + (document.path.parent / Path(value)).relativeTo(routing.route).pathString
                                }
                            }
                            ?.let { writer.write(it, prettyPrint = false) }
                    }
            }
        }.joinAll()
    }

    suspend fun run(args: Array<String>) {
        val type = args.getOrNull(0)
        val devMode = args.getOrNull(1) == "dev"
        when (type) {
            "generate" -> generate(devMode = devMode)
            "serve" -> startServerAndWatch()

            else -> {
                println("Pass a command, [generate, serve]")
            }
        }
    }

    fun startServer(
        configure: Application.() -> Unit = {},
    ) {
        println("Starting server")
        embeddedServer(
            CIO,
            port = port,
            host = "localhost",
        ) {
            install(WebSockets)
            routing {
                webSocket("/ping") {
                    val job = launch {
                        generatorFlow.collectLatest {
                            println("Sending reload")
                            send(Frame.Text("reload"))
                        }
                    }
                    try {
                        incoming.receiveAsFlow().collect()
                    } finally {
                        job.cancel()
                    }
                }
                get("/assets/scripts/autoreload.js") {
                    call.respondText(
                        autoReloadScript(),
                        contentType = ContentType.Text.JavaScript
                    )
                }
                staticFiles("/", dest.toFile()) {
                    extensions("html")
                }
            }
            configure(this)
        }.start(wait = true)
    }

    suspend fun startServerAndWatch(): Unit = withContext(Dispatchers.IO) {
        launch {
            callbackFlow {
                val watcher = DirectoryWatcher.builder()
                    .logger(NOPLogger.NOP_LOGGER)
                    .paths(watch + Path("src"))
                    .listener { event ->
                        trySend(event)
                    }
                    .build()

                watcher.watchAsync()

                awaitClose { watcher.close() }
            }
                .filter { !it.path().endsWith("~") }
                .debounce(300.milliseconds)
                .collectLatest { event ->
                    rebuild()
                    generatorFlow.emit(Unit)
                }
        }

        launch {
            generate(devMode = true)
            startServer()
        }
    }

    val queue = Dispatchers.IO.limitedParallelism(1)

    suspend fun rebuild() = withContext(queue) {
        println("Rebuilding...")
        val amperExists = Path("amper").exists()
        measureTime {
            (if (amperExists) ProcessBuilder("./amper", "run", "generate", "dev")
            else ProcessBuilder(
                "./gradlew",
                "run",
                "--args=generate dev",
                "--parallel",
                "--configuration-cache",
                "--build-cache"
            )).apply {
                environment()["JAVA_HOME"] = System.getProperty("java.home")
//            redirectInput(ProcessBuilder.Redirect.INHERIT)
//            redirectOutput(ProcessBuilder.Redirect.INHERIT)
                redirectError(ProcessBuilder.Redirect.INHERIT)
            }.start().onExit().join()
        }.let { println("Rebuilt in: $it") }
    }
}
