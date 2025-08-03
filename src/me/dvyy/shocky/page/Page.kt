package me.dvyy.shocky.page

import kotlinx.html.HTML
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.html
import me.dvyy.shocky.Site
import org.w3c.dom.Document

/**
 * Represents a page currently being generated using its [html] Document reference..
 *
 * This reference is available only to the page itself,
 * other pages can only be seen as a [PageReference].
 */
data class Page(
    val site: Site,
    val meta: PageMeta,
) {
    val content = meta.content
    var htmlConsumer = createHTMLDocument()
    var html: Document? = null

    inline fun html(crossinline block: HTML.() -> Unit) {
        html = htmlConsumer.html { block() }
    }
}
