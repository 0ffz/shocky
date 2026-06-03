plugins {
    kotlin("jvm")
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
        create("tailwind") {
            id = "me.dvyy.shocky.generator"
            implementationClass = "me.dvyy.shocky.ShockyPlugin"
        }
    }
}
