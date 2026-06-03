package me.dvyy.shocky

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.dvyy.shocky.configuration.Assets
import me.dvyy.shocky.page.Pages
import me.dvyy.shocky.routes.Templates
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.inputStream

data class Site(
    val layout: ShockyDirectories,
    val yaml: Yaml = Yaml(configuration = YamlConfiguration(strictMode = false)),
    val assets: Assets = Assets(),
    val templates: Templates = Templates(),
    val deps: DependencyHolder = DependencyHolder(),
) : Layout {
    val pages: Pages = Pages(this)

    internal var beforeGenerate: () -> Unit = {}
    internal var afterGenerate: () -> Unit = {}
    var rootUrl: String = ""
    var dateFormat = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); dayOfMonth(); chars(", "); year()
    }

    fun templates(block: Templates.() -> Unit) {
        templates.apply(block)
    }


    fun pages(block: Pages.() -> Unit) {
        pages.apply(block)
    }

    fun assets(block: Assets.() -> Unit) {
        assets.apply(block)
    }

    fun beforeGenerate(block: () -> Unit) {
        beforeGenerate = block
    }

    fun afterGenerate(block: () -> Unit) {
        afterGenerate = block
    }

    inline fun <reified T> readFile(
        path: String,
        serializer: KSerializer<T> = serializer<T>(),
    ): T {
        return readFile(layout.source / path, serializer)
    }

    inline fun <reified T> readFile(
        path: Path,
        serializer: KSerializer<T> = serializer<T>(),
        default: () -> T = { error("File not found at path $path") },
    ): T {
        return runCatching {
            yaml.decodeFromStream(serializer, path.inputStream())
        }.getOrElse { default() }
    }
}
