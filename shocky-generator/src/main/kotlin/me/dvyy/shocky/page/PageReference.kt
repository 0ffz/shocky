package me.dvyy.shocky.page

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.yamlMap
import kotlinx.datetime.LocalDate
import kotlinx.serialization.serializer
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.useLines
import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class PageReference(
    val url: String,
    val outputPath: Path,
    val inputFile: Path,
) {
    fun readFrontMatter(): Page {
        return read(readFrontMatter(inputFile), "")
    }

    fun read(): Page {
        val (frontMatter, content) = readFile(inputFile)
        return read(frontMatter, content)
    }

    private fun read(frontMatter: String, content: String): Page {
        val frontMatterNode = yaml.parseToYamlNode(frontMatter.ifEmpty { "{}" }).yamlMap
        val common = yaml.decodeFromYamlNode(CommonFrontMatter.serializer(), frontMatterNode)

        return Page(
            yaml = yaml,
            frontMatter = frontMatterNode,
            content = content,
            title = common.title ?: inputFile.nameWithoutExtension,
            desc = common.desc,
            url = url,
            template = common.template,
            date = common.date?.let { LocalDate.parse(it) },
            tags = common.tags,
        )

    }

    companion object {
        val yaml = Yaml(
            configuration = YamlConfiguration(
                strictMode = false,
            )
        )

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

        fun readFile(input: Path): FileContent {
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
    }
}

data class FileContent(
    val frontMatter: String,
    val content: String,
)
