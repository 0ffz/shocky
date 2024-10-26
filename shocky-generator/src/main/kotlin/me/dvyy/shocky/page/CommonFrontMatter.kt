package me.dvyy.shocky.page

import kotlinx.serialization.Serializable

@Serializable
data class CommonFrontMatter(
    val title: String? = null,
    val desc: String? = null,
    val url: String? = null,
    val date: String? = null,
    val template: String? = null,
    val tags: List<String> = listOf(),
)
