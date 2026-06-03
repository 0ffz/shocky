package me.dvyy.shocky.page

import io.ktor.util.*
import me.dvyy.shocky.Site
import me.dvyy.shocky.configuration.AssetSource
import me.dvyy.shocky.parsing.readMarkdown
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.pathString

class Pages(
    private val site: Site,
) {
    typealias PageBuilder = MutablePage.() -> Unit
    typealias PageParser = context(Site) MutablePage.(Path, InputStream) -> Unit

    // sort by paths to avoid jumping too much in filesystem when parsing
    private val pageBuilders = sortedMapOf<Path, PageBuilder>()
    private val loadedPages = sortedMapOf<Path, Page>()
    internal var defaultParser: PageParser = { path, source ->
        if (path.extension == "md") readMarkdown(source)
        else exclude = true
    }

    fun parser(block: PageParser) {
        defaultParser = block
    }

    operator fun get(path: String): Page? = get(Path(path))

    operator fun get(path: Path): Page? {
        val path = path.normalizeAndRelativize()
        loadedPages[path]?.let { return it.takeIf { !it.exclude } }
        pageBuilders[path]?.let { return load(path, it).takeIf { !it.exclude } }
        return null
    }

    fun add(path: Path, builder: PageBuilder) {
        pageBuilders[path] = builder
    }

    fun addParsing(source: AssetSource, parser: PageParser? = defaultParser) {
        AssetSource.forEachAsset(site.layout, listOf(source)) { asset ->
            add(asset.sitePath) {
                parser?.invoke(site, this, asset.sitePath, asset.getStream())
            }
        }
    }

    fun forEach(
        block: (Path, Page) -> Unit,
    ) {
        pageBuilders.entries.forEach { (path, builder) ->
            block(path, load(path, builder))
        }
    }

    fun <T> map(
        block: (Path, Page) -> T,
    ): List<T> = buildList {
        this@Pages.forEach { path, page -> add(block(path, page)) }
    }

    fun walk(path: String = ""): Sequence<Page> = pageBuilders
        .filterKeys { it.pathString.startsWith(path) }
        .keys
        .asSequence()
        .mapNotNull { get(it) }

    private fun load(path: Path, builder: PageBuilder): Page {
        val mutablePage = MutablePage(site.deps.child(), path).apply(builder)
        val page = Page(site, mutablePage)
        loadedPages[path] = page
        return page
    }

}
