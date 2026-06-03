package me.dvyy.shocky.page

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.serialization.serializer
import me.dvyy.shocky.DependencyHolder
import me.dvyy.shocky.Site
import kotlin.io.path.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Represents a page currently being generated using its [html] Document reference.
 *
 * This reference is available only to the page itself,
 * other pages can only be seen as a [PageReference].
 */
data class Page(
    val site: Site,
    private val provider: MutablePage,
) {
    val deps: DependencyHolder = provider.deps
    val path = provider.path
    val frontMatter = provider.frontMatter
    val title = provider.title ?: path.nameWithoutExtension.capitalize()
    val content: String = provider.content ?: ""
    val date: LocalDate? = provider.date
    val description = provider.description
    val tags = provider.tags
    val formattedDate = date?.format(site.dateFormat)
    val template = provider.template ?: site.templates.default
    val exclude = provider.exclude

    val htmlConsumer = createHTML().let { baseConsumer ->
        object : TagConsumer<String> by baseConsumer {
            override fun onTagStart(tag: Tag) {
                baseConsumer.onTagStart(tag)
                if (site.layout.devMode && tag is HEAD) {
                    baseConsumer.script(src = "/assets/scripts/autoreload.js") {
                        defer = true
                    }
                }
            }
        }
    }


    @PublishedApi
    internal val decodeCache = mutableMapOf<KType, Any?>()

    inline fun <reified T> frontMatter(): T {
        val type = typeOf<T>()
        return decodeCache.getOrPut(type) {
            runCatching {
                site.yaml.decodeFromYamlNode(serializer<T>(), this.frontMatter)
            }.onFailure { it.printStackTrace() }
                .getOrNull()
        } as T
    }

    fun get(key: String): String? = this@Page.frontMatter.getScalar(key)?.content

    var html: String? = null
    val outputFile =
        site.layout.dest / (if (path.nameWithoutExtension == "index") (path.parent ?: Path(".")) / "index.html"
        else (path.parent ?: Path(".")) / "${path.nameWithoutExtension}.html").normalize()
    val url: String = "/" + (provider.url ?: outputFile.relativeTo(site.layout.dest).pathString)

    inline fun html(crossinline block: HTML.() -> Unit) {
        html = htmlConsumer.html { block() }
    }
}

context(page: Page)
val site: Site get() = page.site
