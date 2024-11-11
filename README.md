# Shocky

A super simple static site generator written on top of kotlinx.html and standalone tailwindcss. I use this for my personal site at [0ffz/personal-site](https://github.com/0ffz/personal-site).

Shocky's goal is to provide a simple way to write individual components and programmatically combine them in templates used by markdown files. It does everything at compile time using the typesafe `kotlinx.html` DSL, `kotlinx.serialization` for parsing page frontmatter, and zero JavaScript required on the client.

## Quickstart

Shocky is designed to be used with [Amper](https://github.com/JetBrains/amper), a simpler alternative to Gradle build scripts (you can still depend on it like normal in Gradle, just call `Shocky` in your main class like below.)

#### Install Amper for IntelliJ/Fleet and create a new project using it

See: https://github.com/JetBrains/amper

#### Configure your `module.yaml`

```yaml
product: jvm/app
repositories:
  - id: "mineinabyss"
    url: "https://repo.mineinabyss.com/releases"
dependencies:
  - "me.dvyy:shocky:x.y.z"
settings:
  kotlin:
    serialization: json
  jvm:
    release: 21
```

#### Configure your site in `src/Main.kt`

```kotlin
suspend fun main(args: Array<String>) = Shocky(
    dest = Path("out"),
    route = siteRouting(path = Path("site")) {
        // Set up routing here...
    },
    assets = listOf(Path("site/assets")),
    // If enabled, will auto download and run tailwind standalone binary
    useTailwind = true,
).run(args)
```

#### Serve your site

The Shocky class above reads args passed to let you run the following commands:

- `./amper run serve` - Starts server and refreshes page when src or site are changed.
- `./amper run generate` - Generates site files to the output directory

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
