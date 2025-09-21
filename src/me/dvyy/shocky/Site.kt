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
import kotlin.reflect.KType
import kotlin.reflect.typeOf


class Site(
    val root: Path,
    val pages: Pages,
    val rootUrl: String = "",
    @PublishedApi
    internal val dependencies: Map<KType, Any?>,
) {
    inline fun <reified T> readFile(
        path: String,
        serializer: KSerializer<T> = serializer<T>(),
    ): T {
        return readFile(root / path, serializer)
    }

    inline fun <reified T> inject(): T {
        return (dependencies[typeOf<T>()] ?: error("Dependency of type ${typeOf<T>()} was not injected!")) as T
    }

    companion object {
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
}

context(page: Page)
val site: Site get() = page.site
