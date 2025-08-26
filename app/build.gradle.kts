plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt") // Apply the Kapt plugin
}

android {
    namespace = "com.example.ourapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ourapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildToolsVersion = "35.0.1"

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.appcompat.v171)
    // Other dependencies
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.room.compiler) // For Java
    kapt(libs.androidx.room.room.compiler) // For Kotlin
    implementation(libs.androidx.room.ktx) // Kotlin Extensions

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
}

