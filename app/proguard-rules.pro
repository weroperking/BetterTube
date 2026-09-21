# ============================================================
# BetterTube R8 / ProGuard Rules
# ============================================================

# Keep attributes needed for reflection, annotations, and Kotlin metadata
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod,EnclosingClass

# Keep generic types for JSON parsing
-keepattributes Signature
-keep,allowobfuscation,allowshrinking @com.google.moshi.*
-keep,allowobfuscation,allowshrinking @kotlinx.serialization.*

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
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.AndroidEntryPoint

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
-keep,allowunchecked,allowobfuscation,allowshrinking @retrofit2.*

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
