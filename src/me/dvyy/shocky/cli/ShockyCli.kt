package me.dvyy.shocky.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import me.dvyy.shocky.Shocky
import me.dvyy.shocky.dev.ShockyDevServer
import java.nio.file.Path

class ShockyCli : CliktCommand() {
    override fun run() {
    }
}

class ShockyGenerate(val shocky: Shocky) : CliktCommand("generate") {
    val source: Path by option().path().required()
    val dest: Path by option().path().required()
    val devMode: Boolean by option().boolean().default(false)
    val gradleTask by option().default("generate")
    override fun run(): Unit = runBlocking {
        shocky.generate(source, dest, devMode)
    }
}

class ShockyServe(val shocky: Shocky) : CliktCommand("serve") {
    val port: Int by option().int().default(8080)
    val source: Path by option().path().required()
    val dest: Path by option().path().required()
    val gradleTask by option().default("generate")
    val devMode: Boolean by option().boolean().default(false)

    override fun run() = runBlocking {
        shocky.generate(source, dest, devMode = true)
        println("Starting server with output dest $dest")
        ShockyDevServer(port, dest, gradleTask).startServerAndWatch()
    }
}
