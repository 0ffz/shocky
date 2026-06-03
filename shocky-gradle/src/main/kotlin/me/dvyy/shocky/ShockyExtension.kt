package me.dvyy.shocky

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

interface ShockyExtension {
    val name: String

    val outputDir: DirectoryProperty
    val source: DirectoryProperty
    val mainClass: Property<String>
    var classpath: FileCollection?

    @get:Nested
    val tailwind: TailwindExtension

    fun tailwind(configure: TailwindExtension.() -> Unit) {
        tailwind.apply(configure)
    }
}
