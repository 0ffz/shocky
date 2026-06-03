plugins {
    kotlin("jvm")
    alias(miaLibs.plugins.mia.publication)
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    compileOnly(miaLibs.gradle.kotlin)
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("shocky") {
            id = "me.dvyy.shocky.generator"
            implementationClass = "me.dvyy.shocky.ShockyPlugin"
        }
    }
}
