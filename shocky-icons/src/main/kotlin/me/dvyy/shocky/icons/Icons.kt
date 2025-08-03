package me.dvyy.shocky.icons

import kotlin.text.replace

object Icons {
    //TODO generate references in code

    /**
     * Replaces :icon: references in string with <svg> tags from tabler icons,
     * gives each svg an `icon` css class.
     */
    fun renderFromMarkdown(text: String): String = text.replace(":[\\w-]+:".toRegex()) {
        val icon = it.value.removeSurrounding(":")
        val iconText = readResource("/icons/tabler/$icon.svg")
        iconText?.replace("\n", "")
            ?.replace("<svg", "<svg class=\"icon\"") ?: ":$icon:"
    }

    private fun readResource(path: String): String? {
        return javaClass.getResource(path)?.readText()
    }
}
