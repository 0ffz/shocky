package me.dvyy.shocky

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface TailwindExtension {
    val inputFile: RegularFileProperty
    val outputFile: RegularFileProperty
    val version: Property<String>
}
