// App module — WebView wrapper for DreamKorea SmartClass
// Loads the Next.js web app inside a native Android WebView.
// No Rust/NDK needed — the web app IS the student app.

plugins {
    id("com.android.application")
}

android {
    namespace = "app.dreamkorea.smartclass"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.dreamkorea.smartclass"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
}
