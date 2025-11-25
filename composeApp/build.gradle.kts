import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.JavaVersion

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(11)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation("io.ktor:ktor-client-okhttp:2.3.8")
            implementation("com.google.android.gms:play-services-ads:24.7.0")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.compose.splitpane)
            implementation(libs.compose.dnd)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("io.ktor:ktor-client-core:2.3.8")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.8")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
            implementation(compose.materialIconsExtended)
            // Moved Bouncy Castle to commonMain to be shared
            implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
            implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            // Include native dependencies for all supported platforms to ensure the Uber JAR is cross-platform
            implementation(compose.desktop.linux_x64)
            implementation(compose.desktop.windows_x64)
            implementation(compose.desktop.macos_x64)
            implementation(compose.desktop.macos_arm64)
            
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("io.ktor:ktor-client-okhttp:2.3.8")
            implementation("io.ktor:ktor-server-core:2.3.8")
            implementation("io.ktor:ktor-server-netty:2.3.8")
            implementation("io.ktor:ktor-server-websockets:2.3.8")
            implementation("io.ktor:ktor-server-call-logging-jvm:2.3.8")
            implementation("com.github.kwhat:jnativehook:2.2.2")
            implementation("com.fifesoft:rsyntaxtextarea:3.6.0")
            implementation("com.formdev:flatlaf:3.4.1")
            implementation("com.kitfox.svg:svg-salamander:1.0")
            implementation("org.json:json:20250517")
            implementation("org.slf4j:slf4j-simple:2.0.13")
        }
    }
}

android {
    namespace = "com.kapcode.open.macropad.kmps"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "com.kapcode.open.macropad.kmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 4
        versionName = "1.0.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/*.RSA"
            excludes += "META-INF/*.SF"
            excludes += "META-INF/*.DSA"
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

// Force all configurations to use the same version of coroutines to avoid NoSuchMethodError
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    }
}

compose.desktop {
    application {
        mainClass = "switchdektoptocompose.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "OpenMacropadServer"
            packageVersion = "1.0.0"
        }
    }
}

// Custom task to strip signature files from the generated Uber JAR
tasks.register<Jar>("stripSignaturesFromUberJar") {
    dependsOn("packageUberJarForCurrentOS")
    val uberJarPath = layout.buildDirectory.file("compose/jars/OpenMacropadServer-linux-x64-1.0.0.jar")
    
    // Define output JAR location
    archiveFileName.set("OpenMacropadServer-linux-x64-1.0.0-unsigned.jar")
    destinationDirectory.set(layout.buildDirectory.dir("compose/jars"))

    // Use the original Uber JAR as input
    from(zipTree(uberJarPath)) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    manifest {
        attributes["Main-Class"] = "switchdektoptocompose.MainKt"
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// This block will execute after the main packaging task is done.
tasks.findByName("packageDistributionForCurrentOS")?.let {
    it.doLast {
        val osName = System.getProperty("os.name").lowercase()
        if (osName.contains("linux")) {
            val libDir = project.layout.buildDirectory.dir("compose/binaries/main/app/lib/app")
            libDir.get().asFile.listFiles()?.forEach { file ->
                if (file.name.contains("JNativeHook") && file.name.endsWith(".so")) {
                    println("Making JNativeHook library executable: ${file.name}")
                    exec {
                        commandLine("chmod", "+x", file.absolutePath)
                    }
                }
            }
        }
    }
}
