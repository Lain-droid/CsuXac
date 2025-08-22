plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.csuxac"
version = "1.0.2"



dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    
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

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    manifest {
        attributes(
            "Plugin-Class" to "com.csuxac.SimpleCsuXacPlugin",
            "Plugin-Version" to version,
            "Plugin-Name" to "CsuXac Core",
            "Plugin-Description" to "Ultimate Anti-Cheat System with Zero-Tolerance Policy",
            "Plugin-Author" to "CsuXac Security Team",
            "Plugin-Website" to "https://csuxac.dev",
            "Plugin-API-Version" to "1.21"
        )
    }
}

tasks.shadowJar {
    archiveBaseName.set("csuxac-core")
    archiveClassifier.set("")
    mergeServiceFiles()
    
    // Exclude Paper API from shadow JAR
    dependencies {
        exclude(dependency("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT"))
    }
}

tasks.processResources {
    inputs.property("version", version)
    filesMatching("plugin.yml") {
        expand("version" to version)
    }
}