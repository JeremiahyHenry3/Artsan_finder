# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Firebase Firestore ---
# Firestore maps documents to these data classes by reflection (no-arg
# constructor + property names), so they must survive obfuscation.
-keep class com.example.artsan_finder.data.model.** { *; }
-keepattributes Signature, *Annotation*

# Keep enum values used in Firestore documents (e.g. UserRole)
-keepclassmembers enum com.example.artsan_finder.data.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Kotlin coroutines / Retrofit / Moshi ship their own consumer rules ---
# Keep readable crash reports without exposing source file names
-keepattributes LineNumberTable
-renamesourcefileattribute SourceFile