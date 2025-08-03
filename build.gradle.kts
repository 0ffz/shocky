plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    `maven-publish`
}


repositories {
    mavenCentral()
    maven("https://repo.mineinabyss.com/releases")
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-html-jvm:0.12.0")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    api("com.charleskorn.kaml:kaml:0.78.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines:0.19.2")
    implementation("io.ktor:ktor-server-core:3.1.3")
    implementation("io.ktor:ktor-server-cio:3.1.3")
    implementation("io.ktor:ktor-server-websockets:3.1.3")
    implementation("io.ktor:ktor-server-html-builder:3.1.3")
    implementation("org.jetbrains:markdown:0.7.3")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("io.methvin:directory-watcher:0.18.0")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
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
            name = "mineinabyssMaven"
            val repo = "https://repo.mineinabyss.com/"
            val isSnapshot = System.getenv("IS_SNAPSHOT") == "true"
            val url = if (isSnapshot) repo + "snapshots" else repo + "releases"
            setUrl(url)
            credentials(PasswordCredentials::class)
        }
    }
    publications {
        create<MavenPublication>("java") {
            from(components["java"])
        }
    }
}
