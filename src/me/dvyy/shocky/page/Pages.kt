package me.dvyy.shocky.page

import java.nio.file.Path
import kotlin.io.path.*

object Pages {
    @OptIn(ExperimentalPathApi::class)
    fun walk(path: Path ,siteRoot: Path): List<PageReference> {
        val root = path.normalize()
        if (!root.exists()) return emptyList()
        return root
            .walk()
            .filter { it.isRegularFile() && it.extension == "md" }
            .map { doc -> single(doc, root = siteRoot) }
            .toList()
    }

    fun single(path: Path, root: Path): PageReference {
        val doc = path.normalize()
        val output = outputFor(doc)
        val url = doc.url(root)

        return PageReference(
            url = "/$url",
            inputFile = doc,
            outputPath = output,
        )
    }

    fun outputFor(path: Path): Path =
        if (path.nameWithoutExtension == "index") path.parent / "index.html"
        else path.parent / "${path.nameWithoutExtension}.html"

    private fun Path.url(root: Path): Path =
        (if (nameWithoutExtension == "index") parent else parent / nameWithoutExtension).relativeTo(root)

    fun generate(
        path: Path,
        relativeTo: Path,
        frontMatter: String = "{}",
        content: String = "",
    ): Page {
        val doc = path.normalize()
        val relativeUrl = doc.url(relativeTo)
        return Page.from(frontMatter, content, relativeUrl.toString())
    }
}
