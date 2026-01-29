// Top-level build file
plugins {
    id("com.android.application") version "8.4.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0" apply false
    id("com.github.ben-manes.versions") version "0.48.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.22.0" apply false
    id("com.google.dagger.hilt.android") version "2.46.1" apply false
}

// Apply the versions plugin in the root project so the dependencyUpdates task is available
apply(plugin = "com.github.ben-manes.versions")

