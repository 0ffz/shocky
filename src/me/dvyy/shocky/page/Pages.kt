package me.dvyy.shocky.page

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

class Pages(
    val pages: Map<Path, PageMeta>,
) {
    operator fun get(path: String): PageMeta? = pages[Path(path).normalize()]

    fun walk(path: String = ""): Sequence<PageMeta> = pages
        .filterKeys { it.pathString.startsWith(path) }
        .values
        .asSequence()
}
