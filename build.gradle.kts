plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("de.javagl:jgltf-model:2.0.4")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(19)
}
