rootProject.name = "csuxac"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
        maven("https://plugins.gradle.org/m2/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    }
}

// Performance optimization
gradle.startParameter.isParallelProjectExecutionEnabled = true
gradle.startParameter.maxWorkerCount = Runtime.getRuntime().availableProcessors()