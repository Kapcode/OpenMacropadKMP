# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/kyle/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Ktor rules (common for minification)
-keepattributes Signature
-keepattributes *Annotation*
-keep class io.ktor.** { *; }

# Ignore missing JVM-only management classes that Ktor references for debugger detection
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**

# Compose rules are usually handled by the compiler but sometimes needed
-keep class androidx.compose.runtime.** { *; }
