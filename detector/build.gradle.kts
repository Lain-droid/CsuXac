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
    implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.9.0")
}