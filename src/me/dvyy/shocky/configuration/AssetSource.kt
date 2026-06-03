package me.dvyy.shocky.configuration

import io.ktor.util.*
import me.dvyy.shocky.ShockyDirectories
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.*

data class Asset(
    val sitePath: Path,
    val getStream: () -> InputStream,
)

sealed interface AssetSource {
    data class Directory(val path: Path) : AssetSource
    data class File(val path: Path) : AssetSource
    data class Generated(val path: Path, val content: String) : AssetSource

    //    data class ResourcesFolder(val path: String) : AssetSource
    data class Resource(val path: String) : AssetSource

    data class Multiple(val sources: List<AssetSource>) : AssetSource

    companion object {
        fun forEachAsset(dirs: ShockyDirectories, sources: List<AssetSource>, action: (Asset) -> Unit) {
            sources.forEach { source ->
                when (source) {
                    is File -> {
                        action(Asset(sitePath = source.path) {
                            val fileSystemPath = dirs.source / source.path.normalizeAndRelativize()
                            fileSystemPath.inputStream()
                        })
                    }

                    is Directory -> (dirs.source / source.path.normalizeAndRelativize())
                        .walk()
                        .filter { it.isRegularFile() }
                        .forEach { path -> action(Asset(sitePath = path.relativeTo(dirs.source)) { path.inputStream() }) }

                    is Resource -> {
                        action(Asset(sitePath = Path(source.path)) {
                            this.javaClass.getResourceAsStream("/${source.path}")
                                ?: throw IllegalArgumentException("Resource not found: ${source.path}")
                        })
                    }

                    is Generated -> action(Asset(sitePath = source.path) { source.content.byteInputStream() })

                    is Multiple -> forEachAsset(dirs, source.sources, action)
                }
            }
        }
    }
}
