plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization")
}
//
//val Project.libsRef: VersionCatalog get() = rootProject.extensions.getByType<VersionCatalogsExtension>()
//    .named("shockyLibs")
//    ?: error("shockyLibs version catalog is not defined in settings.gradle!")

tasks{
    register("install") {
        exec {
            commandLine("npm", "install")
        }
    }

    register<JavaExec>("generate") {
        classpath = sourceSets["main"].runtimeClasspath
        args = listOf("build")
        mainClass.set("SiteKt")
    }

    register<JavaExec>("serve") {
        classpath = sourceSets["main"].runtimeClasspath
        args = listOf("serve")
        mainClass.set("SiteKt")

    }
}
