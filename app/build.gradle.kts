plugins {
    alias(libs.plugins.android.application)
    // Removed the manual Kotlin ID to stop the "Extension already registered" crash
}

android {
    namespace = "com.avi.stopgooning"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.avi.stopgooning"
        minSdk = 24
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

    // This block REPLACES the red 'kotlinOptions' line.
    // In AGP 9.1.0, this is the standard way to set the JVM target.
    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    // Hardcoded strings to bypass any potential issues in your libs.versions.toml
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ML Kit & CameraX for the pushup blocker
    implementation("com.google.mlkit:pose-detection:18.0.0-beta3")
    val camerax_version = "1.3.0"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}