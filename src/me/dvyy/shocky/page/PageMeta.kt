package me.dvyy.shocky.page

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.yamlMap
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.serialization.serializer
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.useLines
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Represents immutable information about a page included in the site
 */
data class PageMeta(
    val yaml: Yaml,
    val inputFile: Path?,
    val outputFile: Path,
    val frontMatter: YamlMap,
    val content: String,
    val title: String,
    val desc: String?,
    val url: String,
    val template: String?,
    val tags: List<String> = listOf(),
    val date: LocalDate? = null,
    val templateSelector: Page.() -> Unit = {},
) {
    val decodeCache = mutableMapOf<KType, Any?>()
    val formattedDate get() = date?.format(dateFormat)

    inline operator fun <reified T> invoke(): T {
        val type = typeOf<T>()
        return decodeCache.getOrPut(type) {
            runCatching {
                yaml.decodeFromYamlNode(serializer<T>(), frontMatter)
            }.onFailure { it.printStackTrace() }
                .getOrNull()
        } as T
    }

    fun get(key: String): String? = frontMatter.getScalar(key)?.content

    companion object {
        val yaml = Yaml(
            configuration = YamlConfiguration(
                strictMode = false,
            )
        )

        private val dateFormat = LocalDate.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED); char(' '); dayOfMonth(); chars(", "); year();
        }

        fun fromFileContent(
            file: Path? = null,
            url: String,
            outputFile: Path,
            fileContent: FileContent,
            templateSelector: Page.() -> Unit,
        ): PageMeta {
            val frontMatterNode = yaml.parseToYamlNode(fileContent.frontMatter.ifEmpty { "{}" }).yamlMap
            val common = yaml.decodeFromYamlNode(serializer<CommonFrontMatter>(), frontMatterNode)
            return PageMeta(
                yaml = yaml,
                frontMatter = frontMatterNode,
                content = fileContent.content,
                inputFile = file,
                outputFile = outputFile,
                title = common.title
                    ?: fileContent.content.lineSequence()
                        .firstOrNull()
                        ?.takeIf { it.contains("#") }
                        ?.replace("#+".toRegex(), "")?.trim()
                    ?: file?.nameWithoutExtension ?: "Untitled",
                desc = common.desc,
                url = url,
                template = common.template,
                tags = common.tags,
                date = common.date?.let { LocalDate.parse(it) },
                templateSelector = templateSelector,
            )
        }

        fun fromFile(
            file: Path,
            url: String,
            outputFile: Path,
            templateSelector: Page.() -> Unit,
        ): PageMeta {
            val fileContent = readFile(file)
            return fromFileContent(
                file = file,
                url = url,
                outputFile = outputFile,
                fileContent = fileContent,
                templateSelector = templateSelector,
            )
        }

        private fun readFile(input: Path): FileContent {
            var frontMatter = ""
            val content = input.useLines { lines ->
                val acc = StringBuilder()
                for (line in lines) {
                    if (line == "---") {
                        if (frontMatter.isEmpty()) {
                            frontMatter = acc.toString()
                            acc.clear()
                        }
                    } else {
                        acc.appendLine(line)
                    }
                }
                acc.toString()
            }
            return FileContent(frontMatter, content)
        }

        private fun readFrontMatter(input: Path): String = input.useLines { lines ->
            val acc = StringBuilder()
            for (line in lines) {
                if (line == "---") {
                    if (acc.isNotEmpty()) {
                        return@useLines acc.toString()
                    }
                } else {
                    acc.appendLine(line)
                }
            }
            "{}"
        }

    }
}
