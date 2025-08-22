plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}