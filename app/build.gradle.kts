plugins {
    alias(libs.plugins.android.application)
    // Hilt plugin for dependency injection (Java-compatible)
    alias(libs.plugins.hilt.android.plugin)
    id("com.google.gms.google-services")
}

android {
    namespace = "sevak.hovhannisyan.myproject"
    compileSdk = 36

    defaultConfig {
        applicationId = "sevak.hovhannisyan.myproject"
        minSdk = 27
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
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Core Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Room (Java - annotationProcessor)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Hilt (Java - annotationProcessor)
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    
    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    
    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Charts
    implementation(libs.mpandroidchart)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}