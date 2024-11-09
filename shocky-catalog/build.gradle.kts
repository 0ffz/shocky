plugins {
    `version-catalog`
    `maven-publish`
}

catalog {
    versionCatalog {
        from(rootProject.files("gradle/libs.versions.toml"))
        library("shocky-generator", "me.dvyy:shocky-generator:$version")
        plugin("shocky", "me.dvyy.shocky").version(version.toString())
        bundle("shocky", listOf(
            "shocky-generator",
            "kotlinx-html",
            "kotlinx-serialization-json"
        ))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["versionCatalog"])
            artifactId = "catalog"
        }
    }
}
