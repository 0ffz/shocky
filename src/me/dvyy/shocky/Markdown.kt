package me.dvyy.shocky

import kotlinx.html.*
import org.intellij.lang.annotations.Language
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

val flavour = GFMFlavourDescriptor()

infix fun Tag.markdown(@Language("markdown") src: String) {
    val html = src.markdownToHTML()
    when (this) {
        is HTMLTag -> unsafe { +html }
        is FlowContent -> div { unsafe { +html } }
    }
}

fun String.markdownToHTML(): String {
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(this)
    val html = HtmlGenerator(this, parsedTree, flavour).generateHtml()
    return html
}

infix fun Tag.md(@Language("markdown") src: String) {
    markdown(src)
}
