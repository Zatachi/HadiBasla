plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

// GitHub'daki her derleme numarası yeni bir sürüm kodu olur; böylece güncellemeler üzerine kurulabilir.
val derlemeNo = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.lisanshazirlik.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lisanshazirlik.app"
        minSdk = 26
        targetSdk = 35
        versionCode = derlemeNo
        versionName = "0.1.$derlemeNo"
    }

    // Test imzası: telefona doğrudan kurulan APK'lar için.
    // Play Store'a yüklemeden önce gizli tutulan ayrı bir yükleme anahtarına geçilecek.
    signingConfigs {
        create("test") {
            storeFile = rootProject.file("imza/lisansla-test.jks")
            storePassword = "lisansla"
            keyAlias = "lisansla"
            keyPassword = "lisansla"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("test")
        }
        debug {
            signingConfig = signingConfigs.getByName("test")
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
