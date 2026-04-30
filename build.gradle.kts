plugins {
    kotlin("jvm") version "2.3.21"
}

dependencies {
    implementation("de.javagl:jgltf-model:2.0.4")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(19)
}
