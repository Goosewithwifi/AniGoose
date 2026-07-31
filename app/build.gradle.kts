plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.anigoose.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.anigoose.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        // We only ship native rootfs bootstraps for these ABIs.
        // See bootstrap/README.md for how each bootstrap-<abi>.zip is produced.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Bootstrap zips are large binary payloads, not source — see bootstrap/README.md.
    // They get copied into src/main/assets/bootstrap-<abi>.zip before building.
    androidResources {
        noCompress += listOf("zip")
    }
}

dependencies {
    // Termux's terminal emulator + view widgets (GPLv3). These give us a real
    // VT100-ish TerminalView backed by a pty, without writing our own emulator.
    implementation("com.termux:terminal-emulator:0.118.0")
    implementation("com.termux:terminal-view:0.118.0")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
