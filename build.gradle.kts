plugins {
    alias(miaLibs.plugins.mia.kotlin.jvm)
    alias(miaLibs.plugins.kotlinx.serialization)
    `maven-publish`
}


repositories {
    mavenCentral()
    maven("https://repo.mineinabyss.com/releases")
}

dependencies {
    api(libs.kotlinx.html)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.json)
    api(libs.directory.watcher)
    api(miaLibs.kotlinx.serialization.kaml)
    implementation(miaLibs.kotlinx.coroutines.core)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.flexmark)
    implementation(miaLibs.logback.classic)
    implementation(miaLibs.kermit)
    implementation(miaLibs.console.clikt)
    val graalVersion = "25.0.3" // Use the latest stable version
    implementation("org.graalvm.polyglot:polyglot:$graalVersion")
    implementation("org.graalvm.polyglot:js:$graalVersion")
    implementation("org.graalvm.polyglot:wasm:$graalVersion")
    implementation("org.jsoup:jsoup:1.22.2")
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