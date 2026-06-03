rootProject.name = "shocky"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.mineinabyss.com/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    val miaLibs: String by settings

    repositories {
        maven("https://repo.mineinabyss.com/releases")
    }

    versionCatalogs {
        create("miaLibs") {
            from("com.mineinabyss:catalog:$miaLibs")
        }
    }
}

include("shocky-icons")

include("shocky-gradle")