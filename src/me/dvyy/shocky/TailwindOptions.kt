package me.dvyy.shocky

import java.nio.file.Path

class TailwindOptionsBuilder {
    var enabled: Boolean = true
    var inputCss: Path? = null
    var version: String = "v4.1.7"

    fun build() = TailwindOptions(enabled, inputCss, version)
}

data class TailwindOptions(
    val enabled: Boolean,
    val inputCss: Path?,
    val version: String,
)
