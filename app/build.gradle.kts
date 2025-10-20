plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "vcmsa.projects.wilproject"
    compileSdk = 36

    defaultConfig {
        applicationId = "vcmsa.projects.wilproject"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                substitute(module("com.github.barteksc:android-pdf-viewer"))
                    .using(module("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3"))
                    .because("Original dependency is failing to resolve from JitPack; substituting with maintained fork.")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.androidx.gridlayout)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.ktx.v270)
    implementation(libs.retrofit)
    implementation(libs.androidx.lifecycle.viewmodel.ktx.v270)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android.v173)
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx.v261)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")
    implementation(libs.kotlin.stdlib)
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
    // Other dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx) // Kotlin Extensions

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    //hashing
    implementation(project.dependencies.platform("org.kotlincrypto.hash:bom:0.8.0"))
    implementation("org.kotlincrypto.hash:md")

    implementation("org.kotlincrypto.hash:sha1")


    implementation("org.kotlincrypto.hash:sha2")
    implementation("org.kotlincrypto.hash:sha3")
    implementation("org.kotlincrypto.hash:blake2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.0")
    //ai
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    //gson and retrofit
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation(libs.androidx.activity)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    //epub
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

}