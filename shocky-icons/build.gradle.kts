plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
}

repositories {
    mavenCentral()
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
