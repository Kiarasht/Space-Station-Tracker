# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Restart\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep diagnostic logging in debug builds while removing it from minified
# production builds, including messages that may contain SDK error details.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Hilt / Dagger
-keep class dagger.hilt.internal.aggregatedroot.codegen.*
-keep class *.HiltComponents_*.class
-keep class dagger.hilt.android.WithFragmentBindings
-keep class * { @dagger.hilt.android.AndroidEntryPoint *; }
-keep class * { @dagger.hilt.android.HiltAndroidApp *; }

-keepattributes Signature
-keepattributes *Annotation*

# Ktor's debugger detector checks these desktop JVM management APIs when they
# are available. Android does not provide them, and the guarded references are
# safe to remove during R8 optimization.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
