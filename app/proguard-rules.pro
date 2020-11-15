# Optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Keep everything annotated with @Keep
-keep,allowobfuscation @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Glide rules
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Required metadata for crash reporting
-keepattributes SourceFile,LineNumberTable

# Don't break support libraries
-keep class androidx.appcompat.widget.SearchView { *; }
-keep class com.google.android.material.** { *; }
-keep interface com.google.android.material.** { *; }
-dontwarn android.support.design.**
-keep class androidx.preference.PreferenceCategoryCompat { *; }
-keep class androidx.preference.ColorPreference { *; }
-keep class androidx.preference.IntListPreference { *; }
-keep class androidx.preference.IntDropDownPreference { *; }

# Used to animate values of an Observable with an ObjectAnimator
# (since it calls setters by name using reflection)
-keepnames class * extends androidx.databinding.BaseObservable { *; }

-keep class com.marverenic.** { *; }

# GSON rules
# Don't obfuscate instance field names for GSON
-keepnames class com.marverenic.music.model.** { *; }
# From https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg
-keepattributes Signature
-keep class sun.misc.Unsafe { *; }

## Retrolambda specific rules ##
# as per official recommendation: https://github.com/evant/gradle-retrolambda#proguard
-dontwarn java.lang.invoke.*

## Retrofit + Retrolambda ##
-dontwarn retrofit2.Platform$Java8

## OkHttp ##
-dontwarn okio.**

## RxJava ##
-dontwarn sun.misc.**

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}
