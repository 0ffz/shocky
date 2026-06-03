package me.dvyy.shocky.parsing

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.yamlMap
import kotlinx.datetime.LocalDate
import kotlinx.serialization.serializer
import me.dvyy.shocky.Site
import me.dvyy.shocky.page.CommonFrontMatter
import me.dvyy.shocky.page.MutablePage
import org.intellij.lang.annotations.Language
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.useLines

context(site: Site)
fun MutablePage.readMarkdown(input: InputStream) {
    val (frontMatter, content) = YamlFrontMatterParser(site.yaml).parseMarkdown(input)
    this.content = content
    loadYamlFrontMatter(frontMatter)

    // Set title from first header
    if (title == null) title = content.lineSequence()
        .firstOrNull()
        ?.takeIf { it.contains("#") }
        ?.replace("#+".toRegex(), "")?.trim()
        ?: path.nameWithoutExtension
}

context(site: Site)
fun MutablePage.loadYamlFrontMatter(frontMatter: YamlMap) {
    val common = site.yaml.decodeFromYamlNode(serializer<CommonFrontMatter>(), frontMatter)
    this.frontMatter = frontMatter
    url = common.url
    title = common.title ?: "Untitled"
    date = common.date?.let { LocalDate.parse(it) }
    description = common.desc
    tags.addAll(common.tags)
    common.template?.let { template = site.templates[it] }

}

context(site: Site)
fun MutablePage.loadYamlFrontMatter(@Language("yaml") map: String) {
    loadYamlFrontMatter(site.yaml.parseToYamlNode(map).yamlMap)
}

data class FileContent(
    val frontMatter: YamlMap,
    val content: String,
)

class YamlFrontMatterParser(
    val yaml: Yaml,
) {
    fun parseMarkdown(input: InputStream): FileContent {
        var frontMatter = ""
        val content = input.bufferedReader().useLines { lines ->
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
        val frontMatterNode = yaml.parseToYamlNode(frontMatter.ifEmpty { "{}" }).yamlMap
        return FileContent(frontMatterNode, content)
    }

    fun readFrontMatter(input: Path): String = input.useLines { lines ->
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