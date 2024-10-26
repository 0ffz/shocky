import java.util.*

plugins {
    `kotlin-dsl`
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.kotlinx.serialization)
}

//publishing {
//    repositories {
//        maven {
//            val repo = "https://repo.mineinabyss.com/"
//            val isSnapshot = System.getenv("IS_SNAPSHOT") == "true"
//            val url = if (isSnapshot) repo + "snapshots" else repo + "releases"
//            setUrl(url)
//            credentials {
//                username = project.findProperty("mineinabyssMavenUsername") as String?
//                password = project.findProperty("mineinabyssMavenPassword") as String?
//            }
//        }
//    }
//}
