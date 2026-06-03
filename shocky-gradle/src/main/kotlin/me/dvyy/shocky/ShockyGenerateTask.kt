package me.dvyy.shocky

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

interface TailwindWorkParameters : WorkParameters {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val inputFile: RegularFileProperty

    @get:OutputFile
    val outputFile: RegularFileProperty

    @get:Input
    val version: Property<String>
}

@CacheableTask
abstract class AbstractShockyTask @Inject constructor(
    private val objectFactory: ObjectFactory,
) : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Nested
    abstract val tailwind: TailwindWorkParameters

    @get:Input
    abstract val mainClass: Property<String>

    @get:Input
    abstract val generateTaskName: Property<String>

    @get:Classpath
    abstract var classpath: FileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val source: DirectoryProperty

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor?

    @Inject
    abstract fun getExecOperations(): ExecOperations

    fun tailwind(configure: TailwindWorkParameters.() -> Unit) {
        tailwind.apply(configure)
    }

    init {
        tailwind.version.convention("v4.1.7")
//        buildDir.convention(layout.buildDirectory.dir("shocky/output"))
    }

    protected fun runInternal(isServe: Boolean, devMode: Boolean = isServe) {
//        val buildDir = buildDir.get().asFile.absolutePath
        val buildDir = outputDir.get().asFile.absolutePath
        val sourceDir = source.get().asFile.absolutePath
        outputDir.get().asFile.mkdirs()
        getExecOperations().javaexec {
            it.mainClass.set(mainClass)
            it.args = listOf(
                if (isServe) "serve" else "generate",
                "--dev-mode=${if (devMode) "true" else "false"}",
                "--gradle-task=${generateTaskName.get()}",
                "--dest=${buildDir}",
                "--source=${sourceDir}"
            )
            it.classpath = classpath
        }
        val executor = getWorkerExecutor()!!.noIsolation()
        executor.submit(GenerateTailwindCssTask::class.java) { parameters ->
            parameters.inputFile.set(tailwind.inputFile)
            parameters.outputFile.set(tailwind.outputFile)
        }
//        fileOperations.copy { spec ->
//            spec.from(layout.buildDirectory.dir("shocky/output"))
//            spec.into(outputDir)
//        }
    }
}

@CacheableTask
abstract class ShockyGenerateTask @Inject constructor(
    objectFactory: ObjectFactory,
) : AbstractShockyTask(objectFactory) {

    @get:Input
    @get:Optional
    @get:Option(option = "dev-mode", description = "Run generation in dev mode")
    abstract val devMode: Property<Boolean>

    @TaskAction
    fun run() {
        runInternal(isServe = false, devMode = devMode.getOrElse(false))
    }
}

@CacheableTask
abstract class ShockyServeTask @Inject constructor(
    objectFactory: ObjectFactory,
) : AbstractShockyTask(objectFactory) {
    @TaskAction
    fun run() {
        runInternal(isServe = true)
    }
}