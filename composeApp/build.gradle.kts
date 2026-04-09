import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.JavaVersion
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(11)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // Standardized target name to "jvm" to match folder structure and IDE expectations
    jvm()

    sourceSets {
        // Standard names: jvmMain and jvmTest
        val jvmMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.window)
            implementation(libs.androidx.window.extensions.core)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.google.play.services.ads)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
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
            
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            
            implementation(compose.materialIconsExtended)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.linux_x64)
            implementation(compose.desktop.windows_x64)
            implementation(compose.desktop.macos_x64)
            implementation(compose.desktop.macos_arm64)
            
            implementation(libs.kotlinx.coroutinesSwing)
            
            // Explicit JVM variants for stability in Ktor 3
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.netty)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.server.call.logging)
            
            implementation(libs.jnativehook)
            implementation(libs.rsyntaxtextarea)
            implementation(libs.flatlaf)
            implementation(libs.svg.salamander)
            implementation(libs.json)
            implementation(libs.slf4j.simple)
            implementation(libs.java.keyring)
            
            implementation(libs.bouncycastle.bcpkix)
            implementation(libs.bouncycastle.bcprov)
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
        versionCode = 5
        versionName = "1.1.0"
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
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            // Enable minimal shrinking to remove heavy Ktor-Server classes from the APK
            // This drastically reduces ART verification time at startup.
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

configurations.all {
    // Nuclear Fix: Exclude legacy module that pollutes old Long-based signatures
    exclude(group = "io.ktor", module = "ktor-server-host-common")
}

compose.desktop {
    application {
        mainClass = "switchdektoptocompose.MainKt"
        
        val localProps = Properties()
        val localPropsFile = project.rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { stream -> localProps.load(stream) }
        }
        val keystorePass = localProps.getProperty("keystore.password") ?: "temporary-dev-password"

        jvmArgs("-Dkeystore.password=$keystorePass")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "OpenMacropadServer"
            packageVersion = "1.1.0"
        }
    }
}


tasks.register<Jar>("stripSignaturesFromUberJar") {
    dependsOn("packageUberJarForCurrentOS")
    val uberJarPath = layout.buildDirectory.file("compose/jars/OpenMacropadServer-linux-x64-1.1.0.jar")
    
    archiveFileName.set("OpenMacropadServer-linux-x64-1.1.0-unsigned.jar")
    destinationDirectory.set(layout.buildDirectory.dir("compose/jars"))

    from(zipTree(uberJarPath)) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    manifest {
        attributes["Main-Class"] = "switchdektoptocompose.MainKt"
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.findByName("packageDistributionForCurrentOS")?.let {
    it.doLast {
        val osName = System.getProperty("os.name").lowercase()
        if (osName.contains("linux")) {
            val libDir = project.layout.buildDirectory.dir("compose/binaries/main/app/lib/app")
            libDir.get().asFile.listFiles()?.forEach { file ->
                if (file.name.contains("JNativeHook") && file.name.endsWith(".so")) {
                    println("Making JNativeHook library executable: ${file.name}")
                    file.setExecutable(true)
                }
            }
        }
    }
}
