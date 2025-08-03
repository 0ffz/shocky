package me.dvyy.shocky.page

import java.nio.file.Path

/**
 * A reference to a page on the site. Contains static information about this page,
 * accessible to all other pages. These references are evaluated in one pass,
 * with paths included by [me.dvyy.shocky.routes.RoutesBuilder]
 */
data class PageReference(
    val url: String,
    val outputFile: Path,
    val inputFile: Path,
    val templateSelector: Page.() -> Unit,
) {
}

