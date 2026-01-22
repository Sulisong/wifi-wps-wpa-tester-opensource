# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Hilt
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# LibSu (Root library)
-keep class com.topjohnwu.superuser.** { *; }
-keep class com.topjohnwu.superuser.internal.** { *; }

# Keep algorithm classes (reflection may be used for PIN generation)
-keep class sangiorgi.wps.opensource.algorithm.** { *; }
-keep class sangiorgi.wps.opensource.algorithm.strategy.** { *; }
-keep class sangiorgi.wps.opensource.algorithm.impl.** { *; }

# Keep connection models and callbacks
-keep class sangiorgi.wps.opensource.connection.models.** { *; }
-keep class sangiorgi.wps.opensource.connection.ConnectionUpdateCallback { *; }

# Keep domain models (Parcelable)
-keep class sangiorgi.wps.opensource.domain.models.** { *; }

# Keep data models
-keep class sangiorgi.wps.opensource.data.models.** { *; }

# Keep BuildConfig
-keep class sangiorgi.wps.opensource.BuildConfig { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose
-dontwarn androidx.compose.**

# Keep R8 from stripping interface information
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
