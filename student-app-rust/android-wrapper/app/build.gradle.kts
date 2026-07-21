// App module — wraps the Rust .so (cdylib) into an installable APK.
// Uses NativeActivity (no Kotlin/Java code needed — the Rust android_main
// function is the entry point, driven by android-activity crate).

plugins {
    id("com.android.application")
}

android {
    namespace = "app.eduplatform.student"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.eduplatform.student"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"
        ndk {
            abiFilters += listOf("arm64-v8a")
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
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
            // No Java/Kotlin source needed — NativeActivity + Rust .so
            java.srcDirs(emptyList<String>())
        }
    }
}

// No external dependencies needed — NativeActivity is part of the Android
// framework (android.jar included with compileSdk). The Rust .so is loaded
// via jniLibs, not via any Gradle dependency.
dependencies {
}
