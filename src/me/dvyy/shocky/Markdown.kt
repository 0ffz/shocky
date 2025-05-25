package me.dvyy.shocky

import kotlinx.html.*
import org.intellij.lang.annotations.Language
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

val flavour = GFMFlavourDescriptor()

infix fun Tag.markdown(@Language("markdown") src: String) {
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(src)
    val html = HtmlGenerator(src, parsedTree, flavour).generateHtml()

    when (this) {
        is HTMLTag -> unsafe { +html }
        is FlowContent -> div { unsafe { +html } }
    }
}

infix fun Tag.md(@Language("markdown") src: String) {
    markdown(src)
}
