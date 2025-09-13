plugins {
    // It's good practice to use aliases here too if defined in your catalog for consistency
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) version "2.2.20" apply false // Use the alias
    alias(libs.plugins.ksp) apply false          // Use the alias
}
