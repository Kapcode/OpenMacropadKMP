import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.JavaVersion

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
            implementation(libs.bouncycastle.bcpkix)
            implementation(libs.bouncycastle.bcprov)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
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
            isMinifyEnabled = false
        }
        getByName("debug") {
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

configurations.all {
    // Nuclear Fix: Exclude legacy module that pollutes old Long-based signatures
    exclude(group = "io.ktor", module = "ktor-server-host-common")

    resolutionStrategy {
        // FORCE Ktor 3.0.3 and modern Coroutines globally
        force("io.ktor:ktor-server-core:3.0.3")
        force("io.ktor:ktor-server-netty:3.0.3")
        force("io.ktor:ktor-server-websockets:3.0.3")
        force("io.ktor:ktor-server-call-logging:3.0.3")
        force("io.ktor:ktor-client-core:3.0.3")
        force("io.ktor:ktor-client-okhttp:3.0.3")
        
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.1")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    }
}

compose.desktop {
    application {
        mainClass = "switchdektoptocompose.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "OpenMacropadServer"
            packageVersion = "1.1.0"
        }
    }
}

tasks.register<Exec>("generateDevKeystore") {
    group = "development"
    description = "Generates a self-signed keystore for WSS encryption"

    val outputDir = file("src/jvmMain/resources")
    val keystoreFile = file("${outputDir}/keystore.p12")
    val javaHome = System.getProperty("java.home")
    val keytoolPath = if (System.getProperty("os.name").contains("Windows")) {
        "${javaHome}\\bin\\keytool.exe"
    } else {
        "${javaHome}/bin/keytool"
    }

    doFirst {
        if (!outputDir.exists()) outputDir.mkdirs()
        if (keystoreFile.exists()) keystoreFile.delete()
        println("Generating keystore at: ${keystoreFile.absolutePath}")
    }

    commandLine(
        keytoolPath, "-genkeypair",
        "-alias", "your-alias-name",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-storetype", "PKCS12",
        "-keystore", keystoreFile.absolutePath,
        "-validity", "10000",
        "-storepass", "n678nbccfibliboo",
        "-keypass", "n678nbccfibliboo",
        "-dname", "CN=OpenMacropad, OU=Development, O=Kapcode, L=Unknown, ST=Unknown, C=US",
        "-noprompt"
    )
}

tasks.register<Jar>("stripSignaturesFromUberJar") {
    dependsOn("packageUberJarForCurrentOS")
    val uberJarPath = layout.buildDirectory.file("compose/jars/OpenMacropadServer-linux-x64-1.0.0.jar")
    
    archiveFileName.set("OpenMacropadServer-linux-x64-1.0.0-unsigned.jar")
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
