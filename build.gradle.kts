// Root build.gradle.kts
plugins {
    // Keep empty unless you have root-level plugins
}

allprojects {
    // Avoid adding repositories here — managed in settings.gradle.kts
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
