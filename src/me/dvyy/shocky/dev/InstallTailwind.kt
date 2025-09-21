package me.dvyy.shocky.dev

import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists

/**
 * Gets OS preferred path for storing applications, this is used to cache tailwind verisons.
 */
val shockyInstallPath: Path = run {
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    when {
        osName.contains("win") -> Path(System.getenv("LOCALAPPDATA") ?: "$userHome\\AppData\\Local") / "Shocky"
        osName.contains("mac") -> Path(userHome) / "Library" / "Application Support" / "Shocky"
        else -> Path(userHome) / ".local" / "share" / "shocky"
    }
}.createDirectories()

fun installTailwindIfNecessary(
    dest: Path,
    tailwindVersion: String,
) {
    if(dest.exists()) return

    println("Installing TailwindCSS $tailwindVersion to $dest...")

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

    dest.parent.createDirectories()
    URL(tailwindUrl).openStream().use { input ->
        Files.copy(input, dest)
    }
    if (!osName.contains("win")) {
        dest.toFile().setExecutable(true)
    }
}
