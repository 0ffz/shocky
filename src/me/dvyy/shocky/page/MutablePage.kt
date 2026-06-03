package me.dvyy.shocky.page

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlPath
import io.ktor.util.*
import kotlinx.datetime.LocalDate
import me.dvyy.shocky.DependencyHolder
import me.dvyy.shocky.routes.Template
import org.intellij.lang.annotations.Language
import java.nio.file.Path

/**
 * A provider for all the information needed to create a page.
 *
 * Acts as a way to lazily define all pages before creating them, since they can be interlinked.
 */
class MutablePage(
    val deps: DependencyHolder,
    path: Path,
) {
    val path = path.normalizeAndRelativize()
    var frontMatter = YamlMap(mapOf(), YamlPath.root)
    var title: String? = null
    var url: String? = null
    var description: String? = null
    val tags: MutableList<String> = mutableListOf()
    var date: LocalDate? = null
    var exclude = false

    @Language("markdown")
    var content: String? = null

    var template: Template? = null

}