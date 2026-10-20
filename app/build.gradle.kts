import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// GitHub'daki her derleme numarası yeni bir sürüm kodu olur; böylece güncellemeler üzerine kurulabilir.
val derlemeNo = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

// Aydınlatma metni ve Premium anahtarı metinler/bilgiler.properties dosyasından gelir.
val bilgiler = Properties().apply {
    rootProject.file("metinler/bilgiler.properties").reader(Charsets.UTF_8).use { load(it) }
}
fun bilgi(ad: String) = bilgiler.getProperty(ad).replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.lisanshazirlik.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lisanshazirlik.app"
        minSdk = 26
        targetSdk = 35
        versionCode = derlemeNo
        versionName = "0.2.$derlemeNo"

        buildConfigField("String", "AD_SOYAD", "\"${bilgi("AD_SOYAD")}\"")
        buildConfigField("String", "EPOSTA", "\"${bilgi("EPOSTA")}\"")
        buildConfigField("String", "METIN_TARIHI", "\"${bilgi("TARIH")}\"")
        buildConfigField("int", "AYDINLATMA_SURUM", bilgi("AYDINLATMA_SURUM"))
        buildConfigField("boolean", "PREMIUM_AKTIF", bilgi("PREMIUM_AKTIF"))
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
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
