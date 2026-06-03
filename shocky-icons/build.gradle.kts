import java.net.URI
import java.util.zip.ZipInputStream

plugins {
    alias(miaLibs.plugins.mia.kotlin.jvm)
    alias(miaLibs.plugins.mia.publication)
    `maven-publish`
}

repositories {
    mavenCentral()
}

val downloadTablerIcons by tasks.registering {
    description = "Downloads all outline SVGs from tabler/tabler-icons into resources/icons/tabler."
    group = "build setup"

    val tablerVersion = providers.gradleProperty("tablerIconsVersion").orElse("3.44.0")
    val destDir = layout.projectDirectory.dir("src/main/resources/icons/tabler")
    val downloadDir = layout.buildDirectory.dir("tabler-icons")

    inputs.property("version", tablerVersion)
    outputs.dir(destDir)

    doLast {
        val version = tablerVersion.get()
        val zipUrl = "https://github.com/tabler/tabler-icons/archive/refs/tags/v$version.zip"
        val tmpDir = downloadDir.get().asFile.apply { mkdirs() }
        val zipFile = tmpDir.resolve("tabler-icons-$version.zip")

        logger.lifecycle("Downloading $zipUrl")
        URI(zipUrl).toURL().openStream().use { input ->
            zipFile.outputStream().buffered().use { input.copyTo(it) }
        }

        val out = destDir.asFile
        out.deleteRecursively()
        out.mkdirs()

        val entryRegex = Regex("""^[^/]+/icons/outline/([^/]+\.svg)$""")
        var count = 0
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val match = entryRegex.matchEntire(entry.name)
                if (match != null && !entry.isDirectory) {
                    out.resolve(match.groupValues[1]).outputStream().buffered().use { zip.copyTo(it) }
                    count++
                }
                zip.closeEntry()
            }
        }
        logger.lifecycle("Extracted $count SVGs to ${out.relativeTo(rootDir)}")
    }
}
