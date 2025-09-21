package me.dvyy.shocky.dev

import co.touchlab.kermit.Logger
import co.touchlab.kermit.SimpleFormatter
import co.touchlab.kermit.platformLogWriter
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.dvyy.shocky.ShockyConfiguration
import org.slf4j.helpers.NOPLogger
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

class ShockyDevServer(
    val port: Int,
    val dest: Path,
    val watch: List<Path> = listOf(),
    val init: ShockyConfiguration.() -> Unit,
) {
    val generatorFlow = MutableSharedFlow<Unit>()
    val buildQueue = Dispatchers.IO.limitedParallelism(1)

    fun startServer(
        configure: Application.() -> Unit = {},
    ) {
        Logger.setLogWriters(platformLogWriter(SimpleFormatter))
        Logger.i { "Starting server at http://localhost:$port" }
        embeddedServer(
            CIO,
            port = port,
            host = "localhost",
        ) {
            install(WebSockets.Plugin)
            routing {
                webSocket("/ping") {
                    val job = launch {
                        generatorFlow.collectLatest {
                            Logger.i { "Sending reload" }
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

    suspend fun startServerAndWatch(rebuildSourceFiles: Boolean): Unit = withContext(Dispatchers.IO) {
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
                    if (rebuildSourceFiles) rebuild()
                    else createInstance(isLocalDevServer = true).generate(devMode = true)
                    generatorFlow.emit(Unit)
                }
        }

        launch {
            createInstance(isLocalDevServer = true).generate(devMode = true)
            startServer()
        }
    }


    suspend fun rebuild() = withContext(buildQueue) {
        Logger.i { "Rebuilding..." }
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
        }.let { Logger.i { "Rebuilt in: $it" } }
    }

    suspend fun run(args: Array<String>) {
        val type = args.getOrNull(0)
        val devMode = args.getOrNull(1) == "dev"
        when (type) {
            "generate" -> createInstance(isLocalDevServer = devMode).generate(devMode = devMode)
            "serve" -> startServerAndWatch(rebuildSourceFiles = devMode)

            else -> {
                Logger.i { "Pass a command, [generate, serve]" }
            }
        }
    }

    fun createInstance(
        isLocalDevServer: Boolean,
    ) = ShockyConfiguration(isLocalDevServer).apply(init).build()
}
