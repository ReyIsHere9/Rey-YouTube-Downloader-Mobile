import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

val keystoreProps = Properties()
val keystoreFile = rootProject.file("keystore.properties")
if (keystoreFile.exists()) keystoreProps.load(keystoreFile.inputStream())

android {
    namespace = "org.rey.reyytdlpdw"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.rey.reyytdlpdw"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug { }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        // Extract native libs to disk so the bundled ffmpeg is a real,
        // executable file (yt-dlp runs it as a subprocess).
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"
        // Use the Windows Python locally, but let Linux/CI auto-detect python3.
        val onWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (onWindows) {
            buildPython("C:/Users/AmirDT/AppData/Local/Programs/Python/Python313/python.exe")
        } else {
            buildPython("python3")
        }
        pip {
            install("yt-dlp")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
}

