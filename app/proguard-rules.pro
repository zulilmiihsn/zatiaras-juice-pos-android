# ============================================================
# ZatiarasPOS ProGuard Rules
# ============================================================
# IMPORTANT: Test release builds after any changes to these rules!

# ==================== General ====================

# Keep source file names and line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes Exceptions

# ==================== R8 Missing Classes ====================

# Suppress optional annotation/logging classes not packaged on Android runtime.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn org.slf4j.impl.StaticLoggerBinder

# ==================== Kotlin ====================

# Keep Kotlin Metadata
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ==================== Room Database ====================

# Keep all Room entities
-keep class com.zatiaras.pos.core.data.local.entity.** { *; }

# Keep Room DAOs
-keep interface com.zatiaras.pos.core.data.local.dao.** { *; }

# Keep Room Database
-keep class * extends androidx.room.RoomDatabase { *; }

# Room uses reflection
-dontwarn androidx.room.paging.**

# ==================== Kotlinx Serialization ====================

# Keep serializers
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-dontnote kotlinx.serialization.AnnotationsKt

# Keep serialization classes
-keep,includedescriptorclasses class com.zatiaras.pos.**$$serializer { *; }
-keepclassmembers class com.zatiaras.pos.** {
    *** Companion;
}
-keepclasseswithmembers class com.zatiaras.pos.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serialization JSON classes
-keepclassmembers class kotlinx.serialization.json.** { *; }

# ==================== Remote DTOs ====================

# Keep all DTOs for Supabase
-keep class com.zatiaras.pos.core.data.remote.dto.** { *; }
-keep class com.zatiaras.pos.core.data.remote.*Dto { *; }
-keep class com.zatiaras.pos.core.data.remote.*Dto$* { *; }

# ==================== Supabase / Ktor ====================

# Keep Supabase classes
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# Keep Ktor classes
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# CIO Engine
-keep class io.ktor.client.engine.cio.** { *; }

# ==================== Hilt / Dagger ====================

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }

# Keep @Inject constructors
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep @HiltViewModel
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ==================== WorkManager ====================

# Keep WorkManager workers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Keep HiltWorker
-keep class * extends androidx.work.ListenableWorker {
    @dagger.assisted.AssistedInject <init>(...);
}

# ==================== Jetpack Compose ====================

# Keep Composable functions (already handled by Compose compiler)
-dontwarn androidx.compose.**

# ==================== DataStore ====================

-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ==================== Security Crypto ====================

-keep class androidx.security.crypto.** { *; }

# ==================== Timber ====================

-dontwarn org.jetbrains.annotations.**

# ==================== Enums ====================

# Keep all enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==================== Parcelable ====================

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ==================== Native Methods ====================

-keepclasseswithmembernames class * {
    native <methods>;
}

# ==================== Firebase (if used) ====================

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ==================== Coil ====================

-keep class coil.** { *; }
-dontwarn coil.**

# ==================== Debugging (Remove in production if not needed) ====================

# Uncomment to keep all class names for debugging
# -dontobfuscate

# ==================== Aggressive Optimizations ====================

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Remove Timber logs in release (optional - comment out if you want Timber logs)
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
