package me.dvyy.shocky

import co.touchlab.kermit.Logger
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.script
import me.dvyy.shocky.cli.ShockyCli
import me.dvyy.shocky.cli.ShockyGenerate
import me.dvyy.shocky.cli.ShockyServe
import me.dvyy.shocky.configuration.AssetSource
import me.dvyy.shocky.js.ShikiRenderer
import org.jsoup.Jsoup
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.measureTime

/**
 * Entrypoint for running a Shocky site configuration, whether generating or serving a local site.
 */
class Shocky(
    val init: Site.() -> Unit,
) {
    val renderer = ShikiRenderer()
    val highlighter = HtmlSyntaxHighlighter(renderer)
    @OptIn(ExperimentalPathApi::class)
    suspend fun generate(
        source: Path,
        dest: Path,
        devMode: Boolean,
    ) = withContext(Dispatchers.IO) {
        val site = createSite(source, dest, devMode).apply(init)
        site.beforeGenerate()
//        measureTime {
//            if (!devMode) dest.deleteRecursively()
//            dest.createDirectories()
//        }.let { Logger.i { "Cleared output in: $it" } }
        measureTime {
            AssetSource.forEachAsset(site.layout, site.assets.assets) { asset ->
                runCatching {
                    val destFile = site.layout.dest / asset.sitePath.normalizeAndRelativize()
                    destFile.createParentDirectories()
                    destFile.outputStream().use { output ->
                        asset.getStream().copyTo(output)
                    }
                }.onFailure { Logger.e("Could not find asset ${asset.sitePath} in classpath") }
            }
        }.let { Logger.i { "Copied extra inputs in: $it" } }
        val generateTime = measureTime { generateDocuments(site, devMode) }
        Logger.i { "Generated html files in: ${generateTime}" }
        site.afterGenerate()
        renderer.close()
    }

    suspend fun generateDocuments(
        site: Site,
        devMode: Boolean,
    ) = withContext(Dispatchers.IO) {
        val await = mutableListOf<Job>()
        site.pages.forEach { _, page ->
            if (page.exclude) return@forEach
            await += launch {
                page.template.invoke(page)
                page.outputFile.createParentDirectories()
                    .also { if (it.notExists()) it.createFile() }
                    .writer()
                    .use { writer ->
                        page.htmlConsumer.html {
                            head {
                                script(src = "/assets/scripts/autoreload.js") {
                                    defer = true
                                }
                            }
                        }
                        page.html?.let { writer.write(/*highlighter.highlightCodeBlocks(*/it) }
                    }
            }
        }
        await.joinAll()
    }

//    @OptIn(ExperimentalPathApi::class)
//    fun processAsset(source: AssetSource) {
//        when (val it = source) {
//            is AssetSource.Directory -> {
//                if (it.path.exists()) it.path
//                    .copyToRecursively(it.destRoot / it.path.name, followLinks = false, overwrite = true)
//            }
//
//            is AssetSource.ResourcesFolder -> {
//                val destPath = (it.destRoot / it.path)
//                destPath.createParentDirectories()
//                if (destPath.notExists()) destPath.createFile()
//                destPath.outputStream().use { writer ->
//                    this.javaClass.getResourceAsStream("/${source.path}").use { it.copyTo(writer) }
//                }
//            }
//
//            else -> {
//                TODO()
//            }
//        }
//    }

    fun main(args: Array<String>) {
        ShockyCli().subcommands(ShockyGenerate(this), ShockyServe(this)).main(args)
    }

    private fun createSite(
        source: Path,
        dest: Path,
        devMode: Boolean,
    ): Site {
        return Site(layout = ShockyDirectories(source, dest, devMode))
    }
}

class HtmlSyntaxHighlighter(private val shikiRenderer: ShikiRenderer) {
    /**
     * Parses an HTML string, finds all <pre><code> blocks, highlights them with Shiki,
     * and returns the updated HTML string.
     */
    suspend fun highlightCodeBlocks(rawHtml: String): String {
        // Parse the HTML into a DOM tree. Using parseBodyFragment is safer
        // for snippets of HTML that aren't full <html> documents.
        val document = Jsoup.parse(rawHtml)

        // Select all <pre> tags in the document
        val preElements = document.select("pre")

        for (preElement in preElements) {
            val codeElement = preElement.selectFirst("code") ?: continue
            val rawCode = codeElement.wholeText()
            var language = "text" // Default fallback
            for (className in codeElement.classNames()) {
                if (className.startsWith("language-")) {
                    language = className.removePrefix("language-")
                    break
                } else if (className.startsWith("lang-")) {
                    language = className.removePrefix("lang-")
                    break
                }
            }
            val shikiHtml = shikiRenderer.highlight(rawCode, language)
            preElement.after(shikiHtml)
            preElement.remove()
        }

        // Return the modified HTML string (auto-closed tags included)
        return document.html()
    }
}