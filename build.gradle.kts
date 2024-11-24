plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    `maven-publish`
}

group = "me.dvyy"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.mineinabyss.com/releases")
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    api("com.charleskorn.kaml:kaml:0.61.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines:0.19.2")
    implementation("io.ktor:ktor-server-core:3.0.0")
    implementation("io.ktor:ktor-server-cio:3.0.0")
    implementation("io.ktor:ktor-server-websockets:3.0.0")
    implementation("io.ktor:ktor-server-html-builder:3.0.0")
    implementation("org.jetbrains:markdown:0.7.3")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("io.methvin:directory-watcher:0.18.0")
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    main {
        kotlin.srcDirs("src")
    }
}

publishing {
    repositories {
        maven {
            name = "mineinabyss"
            credentials(PasswordCredentials::class)
            url = uri("https://repo.mineinabyss.com/releases")
        }
    }
    publications {
        create<MavenPublication>("java") {
            from(components["java"])
        }
    }
}
