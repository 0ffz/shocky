package me.dvyy.shocky.routes

import me.dvyy.shocky.page.Page

class Templates {
    private val empty: Template = {}
    private val templates = mutableMapOf<String, Template>()

    var default: Template
        get() = templates["default"] ?: empty
        set(value) {
            templates["default"] = value
        }

    operator fun get(key: String): Template? = templates[key]

    fun add(key: String, init: Template) {
        templates[key] = init
    }
}

typealias Template = Page.() -> Unit