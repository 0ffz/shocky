package me.dvyy.shocky

import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import javax.inject.Inject
import kotlin.io.path.pathString

abstract class GenerateTailwindCssTask : WorkAction<TailwindWorkParameters> {
    @get:Inject
    abstract val execOperations: ExecOperations

    //    init {
//        val installTailwind = project.tasks.named("installTailwindCSS", InstallTailwindCssTask::class.java)
//        dependsOn(installTailwind)
//    }
    override fun execute() {
        execOperations.exec {
            val input = parameters.inputFile.asFile.get().toPath().pathString
            val output = parameters.outputFile.asFile.get().toPath().pathString
            it.commandLine(listOf(tailwindExecutable.pathString, "-i", input, "-o", output, "--minify"))
        }
    }
}