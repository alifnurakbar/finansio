import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Tulis GROQ_API_KEY dari local.properties ke GroqSecrets.kt (masuk .gitignore).
// Dipakai sebagai pengganti BuildConfig supaya editor selain Android Studio
// (misalnya VS Code) juga bisa membaca nilainya tanpa tanda merah.
run {
    val apiKey = localProperties.getProperty("GROQ_API_KEY", "")
        .replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
    val secretsFile = file("src/main/java/com/finansio/app/data/remote/GroqSecrets.kt")
    val content = """
        |// FILE INI DIBUAT OTOMATIS oleh app/build.gradle.kts dari local.properties.
        |// Jangan diedit dan jangan di-commit (sudah masuk .gitignore).
        |package com.finansio.app.data.remote
        |
        |internal object GroqSecrets {
        |    const val GROQ_API_KEY: String = "$apiKey"
        |}
        |""".trimMargin()
    if (!secretsFile.exists() || secretsFile.readText() != content) {
        secretsFile.parentFile.mkdirs()
        secretsFile.writeText(content)
    }
}

android {
    namespace = "com.finansio.app"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.finansio.app"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.navigation.compose)
    implementation(libs.compose.charts)
    implementation(libs.material.icons.extended)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)
    
    implementation(libs.mlkit.document.scanner)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
}