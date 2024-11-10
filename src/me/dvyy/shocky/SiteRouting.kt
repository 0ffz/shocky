package me.dvyy.shocky

import kotlinx.serialization.serializer
import me.dvyy.shocky.page.Page
import me.dvyy.shocky.page.PageReference.Companion.yaml
import me.dvyy.shocky.page.Pages
import java.nio.file.Path
import kotlin.io.path.*

class SiteRouting(
    rootOrNull: SiteRouting?,
    val route: Path = Path("site"),
    val url: String = "",
) {
    val routingRoot = rootOrNull ?: this
    val assets = mutableListOf<Path>()
    val documents = mutableListOf<Document>()
    private val templates = mutableMapOf<String, Page.() -> Unit>()

    operator fun String.invoke(block: SiteRouting.() -> Unit) {
        SiteRouting(routingRoot, route / this, "$url/$this").apply(block)
    }

    inline fun <reified T> generate(
        path: String = "index",
        meta: T,
        content: String = "",
        html: Page.() -> Unit,
    ) {
        val frontMatter = yaml.encodeToString(serializer<T>(), meta)
        val outputPath = Pages.outputFor(route / "$path.html")
        val page = Pages.generate(route / path, route, frontMatter, content)
        page.apply(html)
        addDocument(Document(outputPath, page))
    }

    inline fun pages(
        path: String = ".",
        init: Page.() -> Unit = { defaultTemplate() },
    ) = Pages.walk(route / path, routingRoot.route).forEach { page ->
        addDocument(Document(page.outputPath, page.read().apply(init)))
    }

    fun page(path: String, init: Page.() -> Unit = { defaultTemplate() }) {
        val ref = Pages.single(route / path, relativeTo = routingRoot.route / url)
        addDocument(Document(ref.outputPath, ref.read().apply(init)))
    }

    @OptIn(ExperimentalPathApi::class)
    fun includeAssets(path: String = ".", extensions: List<String> = listOf("png", "jpg", "jpeg", "avif")) {
        val paths = (route / path).walk().filter { it.extension in extensions }
        paths.forEach { routingRoot.assets.add(it) }
    }

    fun template(name: String, init: Page.() -> Unit) {
        routingRoot.templates[name] = init
    }

    fun addDocument(document: Document) = routingRoot.documents.add(document)

    @PublishedApi
    internal fun Page.defaultTemplate() = routingRoot.templates[template ?: "default"]?.invoke(this)
}

class Document(val path: Path, val page: Page)

inline fun siteRouting(
    path: Path = Path("site"),
    rootUrl: String = "",
    crossinline block: SiteRouting.() -> Unit,
) = SiteRouting(null, path, rootUrl).apply(block)

