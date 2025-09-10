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

-keep class org.eclipse.mat.** { *; }
-keep class com.squareup.leakcanary.** { *; }
-keep class com.squareup.haha.** { *; }
-dontwarn com.squareup.haha.guava.**
-dontwarn com.squareup.haha.perflib.**
-dontwarn com.squareup.haha.trove.**
-dontwarn com.squareup.leakcanary.**

# Hilt / Dagger
-keep class dagger.hilt.internal.aggregatedroot.codegen.*
-keep class *.HiltComponents_*.class
-keep class dagger.hilt.android.WithFragmentBindings
-keep class * { @dagger.hilt.android.AndroidEntryPoint *; }
-keep class * { @dagger.hilt.android.HiltAndroidApp *; }

# Retrofit
-dontwarn retrofit2.Platform$Java8
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# Gson (for Retrofit converter)
-keep class com.restart.spacestationtracker.data.**.remote.** { *; }
-keep class com.google.gson.annotations.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$ImageType { public *; }
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder { }
