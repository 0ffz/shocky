package me.dvyy.shocky

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.html.dom.write
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.measureTime

private fun runCommand(vararg args: String) {
    ProcessBuilder(*args).apply {
        inheritIO()
    }.start().waitFor()
}

class Shocky(
    val dest: Path,
    val route: SiteRouting,
    val assets: List<Path> = listOf(),
    val devMode: Boolean = System.getenv("DEVELOPMENT") == "true",
    val useTailwind: Boolean = true,
) {
    @OptIn(ExperimentalPathApi::class)
    fun generate() {
        measureTime {
            if (!devMode) dest.deleteRecursively()
            dest.createDirectories()
        }.let { println("Cleared output in: $it") }
        if (useTailwind) runCommand("npx", "tailwindcss", "-o", "out/assets/tailwind/styles.css", "--minify")
        measureTime {
            assets
                .forEach { it.copyToRecursively(dest / it.name, followLinks = false, overwrite = true) }
        }.let { println("Copied extra inputs in: $it") }
        measureTime { generateDocuments() }.let { println("Generated html files in: $it") }
    }

    fun generateDocuments() {
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
                    document.page.html?.let { writer.write(it) }
                }
        }
    }

    suspend fun run(args: Array<String>) {
        val type = args.firstOrNull()
        when (type) {
            "build" -> generate()
            "serve" -> {
                generate()
                startServerAndWatch()
            }

            else -> {
                println("Pass a command, build, or serve")
            }
        }
    }

    fun startServer(
        configure: Application.() -> Unit = {},
    ) {
        embeddedServer(
            Netty,
            port = 8080,
            host = "localhost",
        ) {
            routing {
                staticFiles("/", dest.toFile()) {
                    extensions("html")
                }
            }
            configure(this)
        }.start(wait = true)
    }

    suspend fun startServerAndWatch(): Unit = coroutineScope {
        launch {
            startServer()
        }

        launch {
            ProcessBuilder("./gradlew", "-t", "generate").apply {
                environment()["JAVA_HOME"] = System.getProperty("java.home")
                environment()["DEVELOPMENT"] = "true"
                redirectInput(ProcessBuilder.Redirect.INHERIT)
                redirectOutput(ProcessBuilder.Redirect.INHERIT)
                redirectError(ProcessBuilder.Redirect.INHERIT)
            }.start()/*.inputStream.bufferedReader().lineSequence().forEach {
            val line = it.lowercase()
            if(listOf("rebuilding", "build").any { line.startsWith(it) }) {
                println(it)
            }
        }*/
        }
    }
}
