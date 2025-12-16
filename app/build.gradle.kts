plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.10"
    alias(libs.plugins.google.gms.services)
}

android {
    namespace = "com.example.discbase"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ilkuygulamam"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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

    // packagingOptions -> packaging olarak düzeltildi
    packaging {
        resources {
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/dependencies"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    dependencies {
        implementation("com.vanniktech:android-image-cropper:4.6.0")
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.material3)
        implementation("androidx.navigation:navigation-compose:2.7.7")
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)
        debugImplementation(libs.androidx.compose.ui.tooling)
        debugImplementation(libs.androidx.compose.ui.test.manifest)
        implementation("androidx.datastore:datastore-preferences:1.1.1")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        implementation("androidx.compose.material:material-icons-extended:1.6.7")
        implementation("io.coil-kt:coil-compose:2.6.0")
        implementation("androidx.credentials:credentials:1.2.2")
        implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
        implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
        implementation("androidx.activity:activity-compose:1.8.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

        // --- Firebase Kütüphaneleri ---
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.auth.ktx)
        implementation(libs.firebase.firestore.ktx)
        implementation("com.google.firebase:firebase-storage-ktx")
        implementation(libs.google.play.services.auth) // Google Girişi için



    }
}
dependencies {
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.games.activity)
    implementation(libs.androidx.compose.foundation)
}



