import java.net.URL
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

val tailwindVersion = "v3.4.14"
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
val tailwindDestination = layout.buildDirectory.file("tailwindcss/tailwindcss").get().asFile.toPath()

tasks {
    register<Exec>("downloadTailwindCSS") {
        group = "build"
        description = "Downloads the latest Tailwind CSS release based on the current system architecture"

        onlyIf {
            tailwindDestination.notExists()
        }

        doFirst {
            tailwindDestination.parent.createDirectories()
            URL(tailwindUrl).openStream().use { input ->
                Files.copy(input, tailwindDestination)
            }
            if (!osName.contains("win")) {
                tailwindDestination.toFile().setExecutable(true)
            }
        }

        commandLine("echo", "Tailwind CSS downloaded to $tailwindDestination")
    }
}

tasks {
    register<JavaExec>("generate") {
        classpath = sourceSets["main"].runtimeClasspath
        args = listOf("build")
        mainClass.set("SiteKt")
        dependsOn("downloadTailwindCSS")
    }

    register<JavaExec>("serve") {
        classpath = sourceSets["main"].runtimeClasspath
        jvmArgs("-Dio.ktor.development=true")
        environment("DEVELOPMENT", "true")
        args = listOf("serve")
        mainClass.set("SiteKt")
        dependsOn("downloadTailwindCSS")
    }
}
