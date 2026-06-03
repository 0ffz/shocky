package me.dvyy.shocky

import com.vladsch.flexmark.ext.aside.AsideExtension
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.gitlab.GitLabExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import kotlinx.html.*
import org.intellij.lang.annotations.Language
import org.jsoup.Jsoup

infix fun Tag.markdown(@Language("markdown") src: String) {
    val html = src.markdownToHTML()
    when (this) {
        is HTMLTag -> unsafe { +html }
        is FlowContent -> div { unsafe { +html } }
    }
}

object MarkdownGeneration {
    val options = MutableDataSet().apply {
        set(
            Parser.EXTENSIONS, listOf(
                FootnoteExtension.create(),
                GitLabExtension.create(),
                TaskListExtension.create(),
                TablesExtension.create(),
                AsideExtension.create(),
                AttributesExtension.create(),
                TocExtension.create(),
                StrikethroughExtension.create()
            )
        )
    }

    // uncomment to convert soft-breaks to hard breaks
    //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
    val parser: Parser = Parser.builder(options).build()
    val renderer = HtmlRenderer.builder(options).build()


}
fun String.markdownToHTML(): String {
    // You can re-use parser and renderer instances
    val document: Node = MarkdownGeneration.parser.parse(this)
    val html = MarkdownGeneration.renderer.render(document) // "<p>This is <em>Sparta</em></p>\n"
    val cleaned = Jsoup.parseBodyFragment(html).body().html()
    return html
}

fun HTMLTag.safeHtml(string: String) {
    unsafe { +Jsoup.parseBodyFragment(string).body().html() }
}

infix fun Tag.md(@Language("markdown") src: String) {
    markdown(src)
}
