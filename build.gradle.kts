plugins {
    kotlin("jvm") version "1.9.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}