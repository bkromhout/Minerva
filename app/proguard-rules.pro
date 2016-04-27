# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/bkromhout/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
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

# Really no reason to do this and invite trouble since the source is available on Github.
-dontobfuscate

# Make sure our little menu icons hack works.
-keepclasseswithmembers class * {
    void setOptionalIconsVisible(boolean);
}

# AboutLibraries
-keep class .R
-keep class **.R$* {
    <fields>;
}

# Butter Knife 7
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

# epublib
-dontwarn org.xmlpull.v1.**
-dontnote org.xmlpull.v1.**
-keep class org.xmlpull.** { *; }

# EventBus 3
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Guava 19
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe
-keep class com.google.j2objc.annotations.** { *; }
-dontwarn   com.google.j2objc.annotations.**
-keep class java.lang.ClassValue { *; }
-dontwarn   java.lang.ClassValue
-keep class org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement { *; }
-dontwarn   org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Realm
-keep class com.bkromhout.minerva.realm.** { *; }
-keep class io.realm.RealmCollection
-keep class io.realm.OrderedRealmCollection

# Retrolambda
-dontwarn java.lang.invoke.*

# RxJava
-keep class rx.schedulers.Schedulers {
    public static <methods>;
}
-keep class rx.schedulers.ImmediateScheduler {
    public <methods>;
}
-keep class rx.schedulers.TestScheduler {
    public <methods>;
}
-keep class rx.schedulers.Schedulers {
    public static ** test();
}