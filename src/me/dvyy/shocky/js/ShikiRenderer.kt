package me.dvyy.shocky.js

import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Source
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ShikiRenderer : AutoCloseable {
    // Initialize GraalJS with WASM support enabled
    private val context: Context = Context.newBuilder("js", "wasm")
        .option("engine.WarnInterpreterOnly", "false")
        .allowHostAccess(HostAccess.ALL)// Required for WASM buffers in some environments
        .build()

    init {
        // 1. Load the bundled JavaScript from resources
        val bundleJs = this::class.java.getResource("/shiki-bundle.js")?.readText()
            ?: throw IllegalStateException("shiki-bundle.js not found in resources")

        context.eval(Source.create("js", bundleJs))

        // 2. Handle Shiki's asynchronous initialization
        val latch = CountDownLatch(1)

        // Expose the Java latch to the JavaScript environment
        context.getBindings("js").putMember("javaLatch", latch)

        // 3. Trigger init and release latch when the Promise resolves
        val initScript = """
            ShikiBridge.init().then(() => {
                javaLatch.countDown();
            }).catch((err) => {
                console.error("Failed to initialize Shiki:", err);
                javaLatch.countDown();
            });
        """.trimIndent()

        context.eval("js", initScript)

        // Block the Kotlin thread until Shiki is fully loaded (with a safety timeout)
        val initialized = latch.await(10, TimeUnit.SECONDS)
        if (!initialized) {
            throw RuntimeException("Shiki initialization timed out.")
        }
    }

    val threadContext = newSingleThreadContext("shikiThreadContext")

    /**
     * Synchronously renders code to HTML.
     * Safe to call from your Ktor/Spring controller routes.
     */
    suspend fun highlight(code: String, language: String, theme: String = "github-dark"): String =
        withContext(threadContext) {
            val renderFunction = context.eval("js", "ShikiBridge.render")

            try {
                renderFunction.execute(code, language, theme).asString()
            } catch (e: Exception) {
                // Fallback to plain pre/code blocks if rendering fails
                """<pre class="shiki error"><code>${escapeHtml(code)}</code></pre>"""
            }
        }

    override fun close() {
        context.close()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}