package me.dvyy.shocky

import io.ktor.util.*
import java.nio.file.Path
import kotlin.io.path.pathString

data class ShockyDirectories(
    val source: Path,
    val dest: Path,
    val devMode: Boolean,
) {
    fun urlFor(path: Path) = path.normalizeAndRelativize().pathString
}
