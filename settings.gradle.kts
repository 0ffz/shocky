plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "shocky"

include("shocky-catalog", "shocky-generator", "shocky-gradle-plugin")
