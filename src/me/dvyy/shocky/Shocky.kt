package me.dvyy.shocky

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.html.dom.append
import kotlinx.html.dom.write
import kotlinx.html.script
import me.dvyy.shocky.dev.installTailwindIfNecessary
import me.dvyy.shocky.page.Page
import me.dvyy.shocky.page.Pages
import me.dvyy.shocky.routes.RoutesBuilder
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.measureTime

class Shocky(
    val dest: Path,
    val routing: RoutesBuilder,
    val assets: List<AssetSource>,
    val tailwindOptions: TailwindOptions,
    val beforeGenerate: () -> Unit,
    val afterGenerate: () -> Unit,
) {
    val site = Site(
        root = routing.rootPath,
        pages = Pages(routing.pages)
    )

    @OptIn(ExperimentalPathApi::class)
    suspend fun generate(devMode: Boolean) = withContext(Dispatchers.IO) {
        measureTime {
            if (!devMode) dest.deleteRecursively()
            dest.createDirectories()
        }.let { println("Cleared output in: $it") }

        launch {
            beforeGenerate()
            measureTime {
                assets.forEach { processAsset(it) }
            }.let { println("Copied extra inputs in: $it") }
            println("Generated html files in: ${measureTime { generateDocuments(devMode) }}")

            if (tailwindOptions.enabled) {
                val tailwindPath = dest / "../build/tailwind-${tailwindOptions.version}"
                installTailwindIfNecessary(tailwindPath, tailwindOptions.version)
                runCommand(
                    buildList {
                        add(tailwindPath.pathString)
                        val input = tailwindOptions.inputCss?.pathString
                        if (input != null) addAll(listOf("-i", input))
                        addAll(
                            listOf(
                                "-o",
                                tailwindOptions.outputCss?.pathString
                                    ?: (dest / "assets/tailwind/styles.css").pathString,
                                "--minify"
                            )
                        )
                    }
                )
            }

            afterGenerate()
        }
    }

    suspend fun generateDocuments(devMode: Boolean) = withContext(Dispatchers.IO) {
        routing.pages.values.map { pageMeta ->
            val page = Page(site, pageMeta).apply { pageMeta.templateSelector.invoke(this) }
            launch {
                page.meta.outputFile.createParentDirectories()
                    .also { if (it.notExists()) it.createFile() }
                    .writer()
                    .use { writer ->
                        page.html
                            ?.apply {
                                if (devMode) getElementsByTagName("head").item(0)?.append {
                                    script(src = "/assets/scripts/autoreload.js") {
                                        defer = true
                                    }
                                }
                                val nodes = getElementsByTagName("img")
                                for (i in 0 until nodes.length) {
                                    val href = nodes.item(i).attributes.getNamedItem("src")
                                    val value = href.nodeValue
                                    if (!value.startsWith("/") && !value.startsWith("http"))
                                        href.nodeValue =
                                            "/" + (page.meta.outputFile.parent / Path(value)).relativeTo(routing.rootPath).pathString
                                }
                            }
                            ?.let { writer.write(it, prettyPrint = false) }
                    }
            }
        }.joinAll()
    }

    @OptIn(ExperimentalPathApi::class)
    fun processAsset(source: AssetSource) {
        when (val it = source) {
            is AssetSource.Folder -> {
                if (it.path.exists()) it.path
                    .copyToRecursively(it.destRoot / it.path.name, followLinks = false, overwrite = true)
            }

            is AssetSource.ResourcesFolder -> {
                val destPath = (it.destRoot / it.path)
                destPath.createParentDirectories()
                if (destPath.notExists()) destPath.createFile()
                destPath.writer().use { writer ->
                    this.javaClass.getResourceAsStream("/${source.path}")
                        .bufferedReader().use {
                            it.copyTo(writer)
                        }
                }
            }
        }
    }
}
