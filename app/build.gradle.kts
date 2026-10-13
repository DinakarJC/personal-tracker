plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "local.tracker.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "local.tracker.app"
        minSdk = 35
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0-parser-test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions += "capture"
    productFlavors {
        create("manual") {
            dimension = "capture"
            applicationIdSuffix = ".manual"
            manifestPlaceholders["appLabel"] = "Personal Tracker Test"
        }
        create("sms") {
            dimension = "capture"
            manifestPlaceholders["appLabel"] = "Personal Expense Tracker"
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }
ktlint { version.set("1.5.0") }

dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
