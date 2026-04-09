# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/kyle/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Ktor rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class io.ktor.** { *; }

# Ignore missing JVM-only management classes
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**

# Keep Data Models and Serialization
-keepnames class com.kapcode.open.macropad.kmps.network.sockets.model.** { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Compose rules
-keep class androidx.compose.runtime.** { *; }
