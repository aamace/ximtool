plugins {
    application
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("com.gradleup.shadow") version "9.4.1"
}

dependencies {
    implementation("de.javagl:jgltf-model:2.0.4")
    implementation("com.formdev:flatlaf:3.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(19)
}

application.mainClass = "ximtool.gui.GuiMainKt"