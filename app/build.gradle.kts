plugins {
    alias(libs.plugins.android.application)
    // If you use Kotlin sources and have it in version catalog:
    // alias(libs.plugins.kotlin.android)
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

    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    // Core AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // CameraX - stable 1.5.3 (March 2026); change to "1.6.0-rc01" if you want RC features
    val cameraxVersion = "1.5.3"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ML Kit Pose Detection - latest beta (stable enough for most uses)
    implementation("com.google.mlkit:pose-detection:18.0.0-beta5")
    // For higher accuracy (bigger download size):
    // implementation("com.google.mlkit:pose-detection-accurate:18.0.0-beta5")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}