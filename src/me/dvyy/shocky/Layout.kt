package me.dvyy.shocky

import me.dvyy.shocky.configuration.AssetSource
import kotlin.io.path.Path

interface Layout {
    fun resources(vararg paths: String) = AssetSource.Multiple(paths.map { AssetSource.Resource(it) })
    fun file(name: String) = AssetSource.File(Path(name))
    fun dir(name: String) = AssetSource.Directory(Path(name))
    fun generated(path: String, content: String) = AssetSource.Generated(Path(path), content)
}