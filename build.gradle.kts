// Root build.gradle.kts
plugins {
    // Android & Kotlin plugins (only declared here, applied in modules)
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false

    // Dependency updates plugin (optional but handy)
    id("com.github.ben-manes.versions") version "0.51.0"
}

// Clean task for whole project
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
