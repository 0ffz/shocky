plugins {
    alias(libs.plugins.kotlinx.serialization)
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.markdown)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.kaml)
    implementation(libs.kotlinx.serialization.json)
    implementation("ch.qos.logback:logback-classic:1.5.6")

    api(libs.kotlinx.datetime)
    api(libs.kotlinx.html)
}

kotlin {
    jvmToolchain(17)
}

publishing {
    repositories {
        maven {
            val repo = "https://repo.mineinabyss.com/"
            val isSnapshot = System.getenv("IS_SNAPSHOT") == "true"
            val url = if (isSnapshot) repo + "snapshots" else repo + "releases"
            setUrl(url)
            credentials {
                username = project.findProperty("mineinabyssMavenUsername") as String?
                password = project.findProperty("mineinabyssMavenPassword") as String?
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
