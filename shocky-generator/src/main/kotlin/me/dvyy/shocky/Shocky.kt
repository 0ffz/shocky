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
import io.methvin.watcher.DirectoryChangeEvent
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.html.dom.append
import kotlinx.html.dom.write
import kotlinx.html.script
import me.dvyy.shocky.dev.autoReloadScript
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

private fun runCommand(vararg args: String) {
    ProcessBuilder(*args).apply {
        inheritIO()
    }.start().waitFor()
}

class Shocky(
    val dest: Path,
    val route: () -> SiteRouting,
    val assets: List<Path> = listOf(),
    val useTailwind: Boolean = true,
    val port: Int = 8080,
    val watch: List<Path> = listOf(),
) {
    val generatorFlow = MutableSharedFlow<Unit>()

    @OptIn(ExperimentalPathApi::class)
    suspend fun generate(devMode: Boolean) = withContext(Dispatchers.IO) {
        measureTime {
            if (!devMode) dest.deleteRecursively()
            dest.createDirectories()
        }.let { println("Cleared output in: $it") }

        if (useTailwind) {
            launch {
                runCommand("./build/tailwindcss/tailwindcss", "-o", "out/assets/tailwind/styles.css", "--minify")
            }
        }
        launch {
            measureTime {
                assets.forEach { it.copyToRecursively(dest / it.name, followLinks = false, overwrite = true) }
            }.let { println("Copied extra inputs in: $it") }
            println("Generated html files in: ${measureTime { generateDocuments(devMode) }}")
        }
    }

    suspend fun generateDocuments(devMode: Boolean) {
        val route = route()
        route.assets.forEach {
            val dest = dest / it.relativeTo(route.route)
            dest.createParentDirectories()
            it.copyTo(dest, overwrite = true)
        }
        route.documents.forEach { document ->
            val path = dest / document.path.relativeTo(route.route)
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
                        }
                        ?.let { writer.write(it, prettyPrint = false) }
                }
        }
        generatorFlow.emit(Unit)
    }

    suspend fun run(args: Array<String>) {
        val type = args.getOrNull(0)
        val devMode = System.getenv("DEVELOPMENT") == "true"
        when (type) {
            "build" -> generate(devMode = devMode)
            "serve" -> startServerAndWatch()

            else -> {
                println("Pass a command, build, or serve")
            }
        }
    }

    fun startServer(
        configure: Application.() -> Unit = {},
    ) {
        embeddedServer(
            CIO,
            port = port,
            host = "localhost",
            watchPaths = listOf("classes")
        ) {
            install(WebSockets)
            routing {
                webSocket("/ping") {
                    generatorFlow.collectLatest {
                        send(Frame.Text("reload"))
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
            callbackFlow<DirectoryChangeEvent> {
                DirectoryWatcher.builder()
                    .paths(watch)
                    .listener {
                        trySend(it)
                    }
                    .build()
                    .watch()
            }
                .flowOn(Dispatchers.IO)
                .filter { !it.path().endsWith("~") }
                .debounce(100.milliseconds)
                .collectLatest { event ->
                    println("File changed: ${event.path()}, regenerating")
                    generateDocuments(devMode = true)
                }
        }

        launch {
            startServer()
        }

        launch {
            ProcessBuilder("./gradlew", "-t", "build", "-x", "test", "-w", "--no-daemon").apply {
                environment()["JAVA_HOME"] = System.getProperty("java.home")
                environment()["DEVELOPMENT"] = "true"
                redirectInput(ProcessBuilder.Redirect.INHERIT)
                redirectOutput(ProcessBuilder.Redirect.INHERIT)
                redirectError(ProcessBuilder.Redirect.INHERIT)
            }.start()
        }
    }
}
