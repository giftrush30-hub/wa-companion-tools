plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.wacompanion"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.wacompanion"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
