import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.JavaVersion // Import JavaVersion for compileOptions

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization) // <-- Use the alias from libs.versions.toml

}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm() // This corresponds to your desktop target

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            // KTOR ENGINE for Android (OkHttp is a good choice)
            implementation("io.ktor:ktor-client-okhttp:2.3.8")
            // Alternatively, for the default Android HTTP client:
            // implementation("io.ktor:ktor-client-android:2.3.8")

            // AdMob dependency
            implementation("com.google.android.gms:play-services-ads:24.7.0")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // KTOR CLIENT - Core dependencies for shared networking logic
            implementation("io.ktor:ktor-client-core:2.3.8") // Core Ktor client
            implementation("io.ktor:ktor-client-content-negotiation:2.3.8") // For JSON conversion
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8") // For Kotlinx Serialization JSON

            // KOTLINX COROUTINES - Essential for asynchronous operations with Ktor
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // KTOR ENGINE for Desktop JVM (OkHttp is a good choice, or Java HTTP client)
            implementation("io.ktor:ktor-client-okhttp:2.3.8")
            // Alternatively, for the Java HTTP client:
            // implementation("io.ktor:ktor-client-java:2.3.8")
            implementation("com.github.kwhat:jnativehook:2.2.2") // Check Maven Central for the latest stable version
        }
    }
}

android {
    namespace = "com.kapcode.open.macropad.kmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.kapcode.open.macropad.kmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.kapcode.open.macropad.kmp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.kapcode.open.macropad.kmp"
            packageVersion = "1.0.0"
        }
    }
}