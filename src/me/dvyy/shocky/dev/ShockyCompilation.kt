package me.dvyy.shocky.dev

import co.touchlab.kermit.Logger
import io.methvin.watcher.DirectoryWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.dvyy.shocky.Shocky
import org.slf4j.helpers.NOPLogger
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.time.Duration.Companion.milliseconds

class ShockyCompilation {
    suspend fun renderClassLoader(classesDir: Path) {
        val classLoader =
            URLClassLoader(
                arrayOf(
                    classesDir.toAbsolutePath().toFile().toURI().toURL(),
                    Path("/var/home/offz/projects/shocky-docs/build/resources/main").toFile().toURI().toURL()
                ), this::class.java.classLoader
            )
        val userClass = classLoader.loadClass("me.dvyy.shocky.docs.MainKt")
        val renderMethod = userClass.getDeclaredMethod("createDocs")
        val shocky = renderMethod.invoke(null) as Shocky

        val out = Path("/var/home/offz/projects/shocky-docs/out")
        out.createDirectories()
        shocky.generate(
            source = Path("/var/home/offz/projects/shocky-docs/docs"),
            dest = out,
            devMode = true
        )
    }
}

fun main() = runBlocking(Dispatchers.IO) {
    val dest = Path("/var/home/offz/projects/shocky-docs/build/classes/kotlin/main")
    val generatorFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val watchJob = launch {
        Logger.i { "Watching directory $dest for changes..." }
        val watcher = DirectoryWatcher.builder()
            .logger(NOPLogger.NOP_LOGGER)
            .path(dest)
            .listener { event ->
                println("$event occurred")
                generatorFlow.tryEmit(Unit)
            }
            .build()
        watcher.watch()
    }
    generatorFlow.debounce(300.milliseconds).collectLatest {
        println("Generating!")
//        ShockyCompilation().renderClassLoader(dest)
    }
}