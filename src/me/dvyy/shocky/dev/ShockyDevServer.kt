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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.helpers.NOPLogger
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ShockyDevServer(
    val port: Int,
    val dest: Path,
    val gradleTask: String,
    val debounceTime: Duration = 300.milliseconds,
) {
    private val _generatorFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val generatorFlow = _generatorFlow.debounce(debounceTime)

    private val gradleBinaryName =
        if (System.getProperty("os.name").lowercase().contains("win")) "gradlew.bat" else "./gradlew"

    suspend fun startServerAndWatch(): Unit = withContext(Dispatchers.IO) {
        launch { startContinuousBuild() }
        launch { startServer() }
    }

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
            launch {
                Logger.i { "Watching directory $dest for changes..." }
                val watcher = DirectoryWatcher.builder()
                    .logger(NOPLogger.NOP_LOGGER)
                    .path(dest)
                    .listener { event ->
                        _generatorFlow.tryEmit(Unit)
                    }
                    .build()
                watcher.watch()
            }
            install(WebSockets.Plugin)
            routing {
                webSocket("/ping") {
                    val job = launch {
                        generatorFlow.drop(1).collectLatest {
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

    fun startContinuousBuild() {
        ProcessBuilder(
            gradleBinaryName,
            gradleTask,
            "--dev-mode",
            "--continuous",
            "--parallel",
            "--configuration-cache",
            "--build-cache"
        ).apply {
            environment()["JAVA_HOME"] = System.getProperty("java.home")
//            redirectInput(ProcessBuilder.Redirect.INHERIT)
            redirectOutput(ProcessBuilder.Redirect.INHERIT)
            redirectError(ProcessBuilder.Redirect.INHERIT)
        }.start().onExit().join()
    }
}
