import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    application
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.csuxac"
version = "1.0.0-SNAPSHOT"

// Repositories are configured in settings.gradle.kts

val kotlinVersion = "1.9.20"
val coroutinesVersion = "1.7.3"
val atomicfuVersion = "0.22.0"
val ktorVersion = "2.3.7"
val serializationVersion = "1.6.0"
val logbackVersion = "1.4.11"
val junitVersion = "5.10.0"
val mockkVersion = "1.13.8"

dependencies {
    // Kotlin core
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Coroutines and concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serializationVersion")
    
    // Network and I/O
    implementation("io.ktor:ktor-network:$ktorVersion")
    implementation("io.ktor:ktor-network-tls:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    
    // Security and cryptography
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.77")
    implementation("org.bouncycastle:bcutil-jdk18on:1.77")
    
    // Database and caching
    implementation("redis.clients:jedis:5.0.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    // Machine Learning
    implementation("org.apache.commons:commons-math3:3.6.1")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    
    // Configuration
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.3")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.7.3")
    
    // Metrics and monitoring
    implementation("io.micrometer:micrometer-core:1.12.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.0")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")
    testImplementation("ch.qos.logback:logback-classic:$logbackVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        languageVersion = "1.9"
        apiVersion = "1.9"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=kotlinx.atomicfu.ExperimentalAtomicfu"
        )
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    
    // Performance testing configuration
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

application {
    mainClass.set("com.csuxac.CsuXacApplicationKt")
}

// Performance optimization
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=200",
        "-XX:+UseStringDeduplication"
    ))
}

// Security hardening
tasks.withType<Jar> {
    manifest {
        attributes(
            "Main-Class" to "com.csuxac.CsuXacApplicationKt",
            "Implementation-Title" to "CsuXac Anti-Cheat",
            "Implementation-Version" to version,
            "Implementation-Vendor" to "CsuXac Security Team",
            "Built-By" to System.getProperty("user.name"),
            "Built-Date" to System.currentTimeMillis().toString(),
            "Built-JDK" to System.getProperty("java.version")
        )
    }
}

// Code coverage - disabled for now
// kover {
//     coverageEngine.set(kotlinx.kover.api.CoverageEngine.JACOCO)
//     jacocoEngineVersion.set("0.8.10")
// }

// Performance benchmarks
tasks.register("benchmark") {
    group = "verification"
    description = "Run performance benchmarks"
    
    dependsOn("test")
    
    doLast {
        exec {
            commandLine("java", "-jar", "build/libs/${project.name}-${project.version}.jar", "--benchmark")
        }
    }
}

// Security audit
tasks.register("securityAudit") {
    group = "verification"
    description = "Run security audit checks"
    
    doLast {
        // Check for known vulnerable dependencies
        exec {
            commandLine("gradle", "dependencyCheckAnalyze")
        }
        
        // Run static analysis
        exec {
            commandLine("gradle", "spotlessCheck")
        }
    }
}