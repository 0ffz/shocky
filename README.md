# Shocky

A super simple static site generator written on top of kotlinx.html and standalone tailwindcss. I use this for my personal site at [0ffz/personal-site](https://github.com/0ffz/personal-site).

Shocky's goal is to provide a simple way to write individual components and programmatically combine them in templates used by markdown files. It does everything at compile time using the typesafe `kotlinx.html` DSL, `kotlinx.serialization` for parsing page frontmatter, and zero JavaScript required on the client.

Currently WIP, will be setting up publishing to a repository soon.

## Quickstart

### Setup

Shocky comes with a gradle plugin and version catalog for simpler setup and running. A template will be available soon, you can look at [0ffz/personal-site](https://github.com/0ffz/personal-site) for an example.

<details>


<summary><b>Manually configure your project...</b></summary>

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

</details>

### Running

The plugin applied above adds the following gradle tasks:

<details>

<summary>
<code>npmInstall</code> <code>generate</code> <code>serve</code>
</summary>

- `gradle npmInstall` - Installs npm dependencies for tailwind
- `gradle generate` - Generates the site to your output directory
- `gradle serve` - Starts a local server to preview your site, runs gradle in watch mode to refresh your site on changes (currently requires page reloads in the browser)

</details>

### Coding your site

#### Create reusable functions for any site compoenent using Tailwind to style:

```kotlin
/** 
 * An outlined chip for a tag list,
 * FlowContent is a common superclass of div, body, etc... in kotlinx.html 
 */
fun FlowContent.outlinedChip(name: String) {
    div("border-2 text-nowrap border-zinc-700 text-zinc-300 text-xs font-semibold uppercase py-1 px-2 rounded-full") {
        +name
    }
}
```

#### Create customizable templates used by markdown files, read frontmatter and render it:

```kotlin
@Serializable
data class CustomMeta(
    val customProperty: String = "default",
)

inline fun Page.myTemplate(
    crossinline init: FlowContent.() -> Unit = { markdown(content) }, // Helper function for rendering markdown pages
) = html {
    val customMeta = meta<CustomMeta>()
    // ...
    body {
        h1 { +page.title }
        p { +customMeta.customProperty }
        init()
    }
}
```

#### Register your templates and site structure in `src/Site.kt`

```kotlin
suspend fun main(args: Array<String>) = Shocky(
    dest = Path("out"),
    route = siteRouting(path = Path("site")) {
        template("default", Page::myTemplate)
        includeAssets() // Include pngs etc.. in site root
        pages(".") // Render all .md iles in site root
        
        // generate example/index.html programmatically
        "example" {
            generate(meta = CommonFrontMatter(title = "Example")) { myTemplate() }
        }
    },
    assets = listOf(Path("site/assets")),
).run(args)

```
