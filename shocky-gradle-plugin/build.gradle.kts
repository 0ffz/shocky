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
