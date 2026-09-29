# ============================================================
# BetterTube R8 / ProGuard Rules
# ============================================================

# Keep attributes needed for reflection, annotations, and Kotlin metadata
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod,EnclosingClass

# Keep generic types for JSON parsing
-keepattributes Signature
-keep,allowobfuscation,allowshrinking @interface com.google.moshi.*

# Moshi JSON Models & RPC Models
-keep class com.bettertube.app.data.aria2.rpc.** { *; }
-keep class com.bettertube.app.domain.model.** { *; }
-keep class com.bettertube.app.data.engine.** { *; }
-keep class **JsonAdapter { *; }
-keepclassmembers class com.bettertube.app.domain.model.** {
    <init>(...);
}

# Dagger / Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class com.bettertube.app.di.** { *; }
-keep class * extends com.bettertube.app.domain.repository.*
-keep class * implements com.bettertube.app.domain.repository.*

# Hilt / Dagger generated code
-keep,allowobfuscation,allowshrinking @interface dagger.hilt.android.AndroidEntryPoint

# Jetpack Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.* { *; }
-keep class androidx.compose.ui.* { *; }

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin Metadata
-keep class kotlin.Metadata { *; }

# FFmpeg and youtubedl-android Native Bindings
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.internal.platform.** { *; }
-keep class org.conscrypt.** { *; }
-keep class org.bouncycastle.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking @interface retrofit2.*

# Moshi Codegen
-keep class com.bettertube.app.data.aria2.rpc.** { *; }
-keepclasseswithmembernames class * {
    @retrofit2.http.*
}

# CrashLogger - keep stack trace info
-keepnames class com.bettertube.app.utils.CrashLogger { *; }

# Strip Debug & Info Logging in Release Builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Preserve standard attributes for reflection & serialization
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# ============================================================
# Apache Commons Compress — required by youtubedl-android
# ============================================================
# Commons Compress uses reflection in ExtraFieldUtils to register
# ZipExtraField implementations at class-load time. R8 must not
# strip their no-arg constructors or the registry init will throw
# "AsiExtraField is not a concrete class" on first ZipFile creation.

-keep class org.apache.commons.compress.archivers.zip.** { *; }
-keep class org.apache.commons.compress.archivers.** { *; }
-keep class org.apache.commons.compress.utils.** { *; }

# Preserve every class that implements ZipExtraField so reflection
# can instantiate them from ExtraFieldUtils' static registry.
-keep class * implements org.apache.commons.compress.archivers.zip.ZipExtraField {
    public <init>();
}

# Preserve the ExtraFieldUtils registry itself.
-keep class org.apache.commons.compress.archivers.zip.ExtraFieldUtils {
    public static <methods>;
    static <fields>;
}

# Explicitly preserve AsiExtraField (the specific class named in the crash).
-keep class org.apache.commons.compress.archivers.zip.AsiExtraField {
    public <init>();
    *;
}

# ============================================================
# youtubedl-android — defensive keep rules
# ============================================================
# The library uses reflection heavily during Python runtime
# initialization and process management. These rules prevent
# the same class of R8-stripping bug from recurring with other
# reflectively-accessed classes.

-keep class com.yausername.** { *; }
-keep class com.yausername.youtubedl_common.** { *; }
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keep class com.yausername.aria2c.** { *; }
-dontwarn com.yausername.**
