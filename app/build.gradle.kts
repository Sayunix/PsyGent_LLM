plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin.get()

}

android {
    namespace = "com.example.psygent"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.psygent"
        minSdk = 21
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {


    // Retrofit & Converter
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // OkHttp Core + Logging
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Compose Core
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.androidx.activity.compose)

    // Tooling
    implementation(libs.ui.tooling.preview)
    implementation(libs.transport.runtime)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // AndroidX Essentials
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // JVM-Logging-Plugin
    implementation(libs.ktor.client.logging.jvm)

    implementation(libs.kotlinx.serialization.json)


}
