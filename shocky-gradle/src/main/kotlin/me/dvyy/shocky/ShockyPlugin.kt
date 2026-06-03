package me.dvyy.shocky

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.Path
import kotlin.io.path.div

private val shockyInstallPath: Path = run {
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    when {
        osName.contains("win") -> Path(System.getenv("LOCALAPPDATA") ?: "$userHome\\AppData\\Local") / "Shocky"
        osName.contains("mac") -> Path(userHome) / "Library" / "Application Support" / "Shocky"
        else -> Path(userHome) / ".local" / "share" / "shocky"
    }
}

internal val tailwindVersion: String = "v4.1.7"
internal val tailwindExecutable = shockyInstallPath / "tailwind" / "tailwind-cli-${tailwindVersion}"

abstract class ShockyPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val installTask = project.tasks.register("installTailwindCSS", InstallTailwindCssTask::class.java)

        val container = project.objects.domainObjectContainer(ShockyExtension::class.java) { name ->
            project.objects.newInstance(ShockyExtension::class.java, name).apply {
                tailwind.version.convention(tailwindVersion)
            }
        }
        project.extensions.add("shocky", container)

        container.all { config ->
            val name = config.name

            val generate = project.tasks.register("${name}Generate", ShockyGenerateTask::class.java) { task ->
                configureShockyTask(task, task.name, installTask, config, project)
            }

            project.tasks.register("${name}Serve", ShockyServeTask::class.java) { task ->
                configureShockyTask(task, generate.name, installTask, config, project)
            }
        }
    }

    private fun configureShockyTask(
        task: AbstractShockyTask,
        generateTaskName: String,
        installTask: TaskProvider<InstallTailwindCssTask>,
        extension: ShockyExtension,
        project: Project,
    ) {
        task.dependsOn(installTask)
        task.outputDir.set(extension.outputDir)
        task.source.set(extension.source)
        task.generateTaskName.set(generateTaskName)
        task.mainClass.set(extension.mainClass)
        task.tailwind.inputFile.set(extension.tailwind.inputFile)
        task.tailwind.outputFile.set(extension.tailwind.outputFile)
        task.tailwind.version.set(extension.tailwind.version)
        task.classpath = project.files(Callable { extension.classpath ?: project.files() })
    }
}
