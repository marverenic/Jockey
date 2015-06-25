# Optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Stop Picasso from causing release builds to fail
-dontwarn com.squareup.okhttp.**

# Don't obfuscate class and method names for easier debugging
-keepnames class com.marverenic.** { *; }

# Don't break support libraries
-keep class android.support.v7.widget.SearchView { *; }
-keep class android.support.design.widget.** { *; }
-keep interface android.support.design.widget.** { *; }
-dontwarn android.support.design.**

# Remove logcat logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
