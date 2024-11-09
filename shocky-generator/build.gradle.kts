plugins {
    alias(libs.plugins.kotlinx.serialization)
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.markdown)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.kaml)
    implementation(libs.kotlinx.serialization.json)
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("io.methvin:directory-watcher:0.18.0")
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.html)
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
