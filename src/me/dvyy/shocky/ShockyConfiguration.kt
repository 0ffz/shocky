package me.dvyy.shocky

import me.dvyy.shocky.dev.ShockyDevServer
import me.dvyy.shocky.routes.RoutesBuilder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.reflect.KType
import kotlin.reflect.typeOf

sealed interface AssetSource {
    data class Folder(val path: Path, val destRoot: Path) : AssetSource
    data class ResourcesFolder(val path: String, val destRoot: Path) : AssetSource
}

data class ShockyConfiguration(
    val isLocalDevServer: Boolean = false,
    var dest: Path = Path("out"),
    var siteRoot: Path = Path("site"),
    var assets: MutableList<AssetSource> = mutableListOf(),
    var port: Int = 8080,
    var siteUrl: String = "",
    var currentDir: Path = Path("."),
    private var beforeGenerate: () -> Unit = {},
    private var afterGenerate: () -> Unit = {},
) {
    val dependencies: MutableMap<KType, Any?> = mutableMapOf()

    private val routing by lazy { RoutesBuilder(siteRoot, dest) }

    private val tailwindOptions = TailwindOptionsBuilder()

    fun tailwind(block: TailwindOptionsBuilder.() -> Unit) {
        tailwindOptions.block()
    }

    fun dest(path: String) {
        dest = Path(path)
    }

    fun siteRoot(path: String) {
        siteRoot = Path(path)
    }

    fun assets(vararg paths: String) {
        assets.addAll(paths.map { AssetSource.Folder(currentDir / it, dest) })
    }

    fun assetsFromResources(vararg sources: String) {
        assets.addAll(sources.map { AssetSource.ResourcesFolder(it, dest) })
    }

    fun assets(vararg sources: AssetSource) {
        assets.addAll(sources)
    }

    fun routing(block: RoutesBuilder.() -> Unit) {
        routing.apply { block() }
    }

    fun beforeGenerate(block: () -> Unit) {
        beforeGenerate = block
    }

    fun afterGenerate(block: () -> Unit) {
        afterGenerate = block
    }

    inline fun <reified T: Any> provide(value: T) {
        dependencies[typeOf<T>()] = value
    }

    fun build() = Shocky(
        dest = dest,
        routing = routing,
        assets = assets,
        tailwindOptions = tailwindOptions.build(),
        siteUrl = if (isLocalDevServer) "" else siteUrl,
        beforeGenerate = beforeGenerate,
        dependencies = dependencies.toMap(),
        afterGenerate = afterGenerate,
    )
}

fun shocky(
    port: Int = 8080,
    dest: Path = Path("out"),
    watch: List<Path> = listOf(),
    init: ShockyConfiguration.() -> Unit,
): ShockyDevServer {
    return ShockyDevServer(
        port = port,
        dest = dest,
        watch = watch,
        init = init
    )
}
