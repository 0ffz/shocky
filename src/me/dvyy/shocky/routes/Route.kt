package me.dvyy.shocky.routes

import java.nio.file.Path
import kotlin.io.path.relativeTo

interface Route {
    val path: Path

    fun relativeTo(path: Path): Path = this.path.relativeTo(path)
}
