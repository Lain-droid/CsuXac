plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.apache.kafka:kafka-clients:3.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}