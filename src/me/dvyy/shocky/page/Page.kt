package me.dvyy.shocky.page

import com.charleskorn.kaml.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.html.HTML
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.html
import kotlinx.serialization.serializer
import me.dvyy.shocky.page.PageReference.Companion.yaml
import org.w3c.dom.Document
import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class Page(
    val yaml: Yaml,
    val frontMatter: YamlMap,
    val content: String,
    val title: String,
    val desc: String?,
    val url: String,
    val template: String?,
    val tags: List<String> = listOf(),
    val date: LocalDate? = null,
) {
    val page = this
    val decodeCache = mutableMapOf<KType, Any?>()

    val formattedDate get() = date?.format(dateFormat)

    @PublishedApi
    internal var html: Document? = null

    inline fun <reified T> meta(): T {
        val type = typeOf<T>()
        return decodeCache.getOrPut(type) {
            runCatching {
                yaml.decodeFromYamlNode(serializer<T>(), frontMatter)
            }.onFailure { it.printStackTrace() }
                .getOrNull()
        } as T
    }

    inline fun html(crossinline block: HTML.() -> Unit) {
        html = createHTMLDocument().html { block() }
    }

    fun get(key: String): String? = frontMatter.getScalar(key)?.content

    companion object {
        private val dateFormat = LocalDate.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); dayOfMonth(); chars(", "); year();
        }

        fun from(
            frontMatter: String = "",
            content: String = "",
            url: String,
        ): Page {
            val meta = yaml.parseToYamlNode(frontMatter.ifEmpty { "{}" }).yamlMap
            return Page(
                yaml = yaml,
                frontMatter = meta,
                content = content,
                title = meta.getOrNull("title") ?: "Untitled",
                desc = meta.getOrNull("desc"),
                template = meta.getOrNull("template"),
                url = "/" + (meta.getOrNull("url") ?: url).removePrefix("/"),
            )
        }

        private fun Map<String, String>.toYamlMap(yamlPath: YamlPath = YamlPath()): YamlMap {
            val yamlEntries = mapKeys { (key, _) -> YamlScalar(key, yamlPath) }
                .mapValues { (_, value) -> YamlScalar(value, yamlPath) }
            return YamlMap(yamlEntries, YamlPath.root)
        }

        private fun YamlMap.getOrNull(key: String) = (get<YamlNode>(key) as? YamlScalar)?.content
    }
}
