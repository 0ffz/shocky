package me.dvyy.shocky

import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import me.dvyy.shocky.page.Page
import me.dvyy.shocky.page.PageMeta.Companion.yaml
import me.dvyy.shocky.page.Pages
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.inputStream


class Site(
    val root: Path,
    val pages: Pages,
) {
    inline fun <reified T> readFile(
        path: String,
        serializer: KSerializer<T> = serializer<T>(),
    ): T {
        return yaml.decodeFromStream(serializer, (root / path).inputStream())
    }
}

context(page: Page)
val site: Site get() = page.site
