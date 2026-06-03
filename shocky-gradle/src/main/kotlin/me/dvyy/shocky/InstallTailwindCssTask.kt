package me.dvyy.shocky

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URL
import java.nio.file.Files
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists

abstract class InstallTailwindCssTask : DefaultTask() {
    @OutputFile
    val dest = tailwindExecutable

    @TaskAction
    fun run() {
        if (dest.exists()) return
        dest.createParentDirectories()
        project.logger.info("Installing TailwindCSS $tailwindVersion to $dest...")

        val tailwindBaseUrl = "https://github.com/tailwindlabs/tailwindcss/releases/download/$tailwindVersion"
        val osName = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        val tailwindFileName = when {
            osName.contains("win") && arch.contains("64") -> "tailwindcss-windows.exe"
            osName.contains("mac") && arch.contains("aarch64") -> "tailwindcss-macos-arm64"
            osName.contains("mac") -> "tailwindcss-macos-x64"
            osName.contains("nix") || osName.contains("nux") -> when {
                arch.contains("64") -> "tailwindcss-linux-x64"
                arch.contains("arm") -> when {
                    arch.contains("v7") -> "tailwindcss-linux-armv7"
                    else -> "tailwindcss-linux-arm64"
                }

                else -> throw IllegalStateException("Unsupported architecture: $arch")
            }

            else -> throw IllegalStateException("Unsupported OS or architecture: $osName $arch")
        }
        val tailwindUrl = "$tailwindBaseUrl/$tailwindFileName"

        URL(tailwindUrl).openStream().use { input ->
            Files.copy(input, dest)
        }
        if (!osName.contains("win")) {
            dest.toFile().setExecutable(true)
        }
    }
}