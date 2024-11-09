plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    `maven-publish`
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "maven-publish")
    publishing {
        repositories {
            maven {
                name = "mineinabyss"
                val repo = "https://repo.mineinabyss.com/"
                val isSnapshot = System.getenv("IS_SNAPSHOT") == "true"
                val url = if (isSnapshot) repo + "snapshots" else repo + "releases"
                setUrl(url)
                credentials(PasswordCredentials::class)
            }
        }
    }
}
