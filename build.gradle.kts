plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.csuxac"
version = "1.0.0"

dependencies {
    // Core Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // Math
    implementation("org.apache.commons:commons-math3:3.6.1")
}

application {
    mainClass.set("com.csuxac.SimpleMainKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.csuxac.SimpleMainKt"
    }
}

tasks.shadowJar {
    archiveBaseName.set("csuxac-core")
    archiveClassifier.set("")
    mergeServiceFiles()
}