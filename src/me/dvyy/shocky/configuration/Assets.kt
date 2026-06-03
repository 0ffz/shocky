package me.dvyy.shocky.configuration

import java.nio.file.Path

class Assets {
    val assets: MutableList<AssetSource> = mutableListOf()
    fun from(vararg sources: AssetSource, outputPath: (Path) -> Path = { it }) {
        assets.addAll(sources)
    }
}
