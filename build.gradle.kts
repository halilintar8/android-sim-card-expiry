// Root build.gradle.kts
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
    id("com.github.ben-manes.versions") version "0.51.0" // ✅ add this
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
