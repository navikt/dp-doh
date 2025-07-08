plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless") version "7.1.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.1.0")
}
