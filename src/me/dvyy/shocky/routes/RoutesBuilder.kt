package me.dvyy.shocky.routes

import co.touchlab.kermit.Logger
import me.dvyy.shocky.page.Page
import me.dvyy.shocky.page.PageMeta
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Defines which pages should be included for rendering, and their routes.
 *
 * @param rootPath The path relative to which to search for included pages.
 */
class RoutesBuilder(
    val rootPath: Path,
    val outputRoot: Path,
) : Route {
    override val path: Path = Path(".")
    val pages = mutableMapOf<Path, PageMeta>()
    private val templates = mutableMapOf<String, Page.() -> Unit>()

//    fun generate(
//        path: Path,
//        relativeTo: Path,
//        frontMatter: String = "{}",
//        content: String = "",
//    ): Page {
//        val doc = path.normalize()
//        val relativeUrl = doc.url(relativeTo)
//        return Page.from(frontMatter, content, relativeUrl.toString())
//    }

    context(route: Route)
    inline operator fun String.invoke(block: context(Route) () -> Unit) {
        val subroute = object : Route {
            override val path: Path = route.path / this@invoke
        }
        block(subroute)
    }

//    inline fun <reified T> generate(
//        path: String = "index",
//        meta: T,
//        content: String = "",
//        html: Page.() -> Unit,
//    ) {
//        val frontMatter = yaml.encodeToString(serializer<T>(), meta)
//        val outputPath = Pages.outputFor(route / "$path.html")
//        val page = Pages.generate(route / path, route, frontMatter, content)
//        page.apply(html)
//        addDocument(Document(outputPath, page))
//    }

    fun template(name: String, init: Page.() -> Unit) {
        templates[name] = init
    }

    context(route: Route)
    fun include(path: String = ".", template: Page.() -> Unit = { defaultTemplate() }) {
        val relativePath = (route.path / path).normalize()
        val absolutePath = rootPath / relativePath

        if (absolutePath.notExists()) {
            Logger.w { "Tried loading page at $relativePath, but it does not exist!" }
            return
        }

        val output = outputFileFor(relativePath)
        val url = relativePath.url()
        val meta = PageMeta.fromFile(
            absolutePath,
            url = "/$url",
            outputFile = output,
            templateSelector = template,
        )
        pages[relativePath] = meta
    }

    @OptIn(ExperimentalPathApi::class)
    context(route: Route)
    fun includeDirectory(path: String, template: Page.() -> Unit = { defaultTemplate() }) {
        val root = (rootPath / route.path / path).normalize()
        if (!root.exists()) return
        root.walk()
            .filter { it.isRegularFile() && it.extension == "md" }
            .forEach { doc -> include(doc.relativeTo(rootPath).pathString, template) }
    }

    @PublishedApi
    internal fun Page.defaultTemplate() = templates[meta.template ?: "default"]?.invoke(this)

    private fun outputFileFor(path: Path): Path =
        outputRoot / (if (path.nameWithoutExtension == "index") (path.parent ?: Path(".")) / "index.html"
        else (path.parent ?: Path(".")) / "${path.nameWithoutExtension}.html").normalize()

    private fun Path.url(): Path =
       (if (nameWithoutExtension == "index") (parent ?: Path(".")) else (parent ?: Path(".")) / nameWithoutExtension)
            .normalize()
}


