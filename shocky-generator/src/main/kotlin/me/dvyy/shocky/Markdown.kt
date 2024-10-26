package me.dvyy.shocky

import kotlinx.html.FlowContent
import kotlinx.html.HTMLTag
import kotlinx.html.unsafe
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

val flavour = CommonMarkFlavourDescriptor()

fun FlowContent.markdown(src: String) {
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(src)
    val html = HtmlGenerator(src, parsedTree, flavour).generateHtml()
    (this as? HTMLTag)?.unsafe {
        +html
    }
}

fun HTMLTag.markdown(src: String) {
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(src)
    val html = HtmlGenerator(src, parsedTree, flavour).generateHtml()
    unsafe {
        +html
    }
}
