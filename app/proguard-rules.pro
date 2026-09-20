# BetterTube ProGuard & R8 Optimization Rules

# Moshi JSON Models & RPC Models
-keep class com.bettertube.app.data.aria2.rpc.** { *; }
-keep class com.bettertube.app.domain.model.** { *; }
-keep class com.bettertube.app.data.engine.** { *; }
-keep class **JsonAdapter { *; }

# Dagger / Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Jetpack Compose
-dontwarn androidx.compose.**

# Kotlin Metadata
-keep class kotlin.Metadata { *; }

# youtubedl-android Native Bindings & FFmpeg/Aria2c
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Strip Debug & Info Logging in Release Builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Preserve standard attributes for reflection & serialization
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
