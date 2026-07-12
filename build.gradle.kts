plugins {
    id("com.android.application") version "9.2.1" apply false
    // AGP 9 provides Kotlin support natively — the kotlin.android plugin is gone.
    // The Compose compiler ships with Kotlin 2.x and replaces the old
    // composeOptions.kotlinCompilerExtensionVersion.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
}
