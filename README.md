# Shocky

A super simple static site generator written on top of kotlinx.html and standalone tailwindcss. I use this for my personal site at [0ffz/personal-site](https://github.com/0ffz/personal-site).

Shocky's goal is to provide a simple way to write individual components and programmatically combine them in templates used by markdown files. It does everything at compile time using the typesafe `kotlinx.html` DSL, `kotlinx.serialization` for parsing page frontmatter, and zero JavaScript required on the client.

Currently WIP, will be setting up publishing to a repository soon.

## Usage

Shocky comes with a gradle plugin and version catalog for simpler setup and running.

### Setup

#### Update your `gradle.properties`
```kotlin
shockyVersion=<version>
```

#### Create a new gradle project, add the following to your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.mineinabyss.com/releases")
    }
}

dependencyResolutionManagement {
    val shockyVersion: String by settings

    repositories {
        maven("https://repo.mineinabyss.com/releases")
    }
    versionCatalogs {
        create("shockyLibs").from("me.dvyy:catalog:$shockyVersion")
    }
}
```

#### Configure your `build.gradle.kts`:

```kotlin
plugins {
    alias(shockyLibs.plugins.shocky)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(shockyLibs.bundles.shocky)
}

sourceSets.main {
    kotlin.srcDirs("src")
    resources.srcDirs("site")
}

kotlin {
    jvmToolchain(17)
}
```

This will let you write components directly in the `src` directory and place markdown files under `site`, which gradle will watch for changes.

#### Configure your site in `src/Site.kt`

```kotlin
suspend fun main(args: Array<String>) = Shocky(
    dest = Path("out"),
    route = siteRouting(path = Path("site")) {
        // Set up routing here...
    },
    assets = listOf(Path("site/assets")),
).run(args)

```

### Running

The plugin applied above adds the following gradle tasks:

- `gradle npmInstall` - Installs npm dependencies for tailwind
- `gradle generate` - Generates the site to your output directory
- `gradle serve` - Starts a local server to preview your site, runs gradle in watch mode to refresh your site on changes (currently requires page reloads in the browser)
