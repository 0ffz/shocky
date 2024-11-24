package me.dvyy.shocky

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div

data class ShockyConfiguration(
    var dest: Path = Path("out"),
    var siteRoot: Path = Path("site"),
    var assets: MutableList<Path> = mutableListOf(),
    var useTailwind: Boolean = true,
    var tailwindVersion: String = "v3.4.14",
    var port: Int = 8080,
    var watch: MutableList<Path> = mutableListOf(),
    var currentDir: Path = Path("."),
    private var routing: SiteRouting = siteRouting { },
    private var beforeGenerate: () -> Unit = {},
    private var afterGenerate: () -> Unit = {},
) {
    fun dest(path: String) {
        dest = Path(path)
    }

    fun siteRoot(path: String) {
        siteRoot = Path(path)
    }

    fun assets(vararg paths: String) {
        assets.addAll(paths.map { currentDir / it })
    }

    fun watch(vararg paths: String) {
        watch.addAll(paths.map { currentDir / it })
    }

    fun routing(block: SiteRouting.() -> Unit) {
        routing = siteRouting(siteRoot) { block() }
    }

    fun beforeGenerate(block: () -> Unit) {
        beforeGenerate = block
    }

    fun afterGenerate(block: () -> Unit) {
        afterGenerate = block
    }

    fun build() = Shocky(
        dest = dest,
        routing = routing,
        assets = assets,
        useTailwind = useTailwind,
        tailwindVersion = tailwindVersion,
        port = port,
        beforeGenerate = beforeGenerate,
        afterGenerate = afterGenerate,
        watch = watch
    )
}

fun shocky(block: ShockyConfiguration.() -> Unit) = ShockyConfiguration().apply(block).build()
