# Keep annotations and signatures for reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Kotlin Coroutines & Continuation (preserves suspend function return types in Retrofit)
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep class * extends kotlin.coroutines.jvm.internal.ContinuationImpl { *; }
-dontwarn kotlinx.coroutines.**

# Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# MovieBox API interface and models (Must not be obfuscated or stripped)
-keep interface com.ofc.movies.data.api.MovieApiService { *; }
-keep class com.ofc.movies.data.api.** { *; }
-keep class com.ofc.movies.data.model.** { *; }
-keep class com.ofc.movies.data.local.** { *; }

# Gson serialization and TypeToken
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# Download manager, service, and data classes
-keep class com.ofc.movies.data.download.** { *; }


