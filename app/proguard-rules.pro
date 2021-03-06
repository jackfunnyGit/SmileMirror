# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\AppData\Local\Android\sdk2/tools/proguard/proguard-android.txt
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
# Basic
-keepclassmembers class **.R$* {
    public static <fields>;
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

-keepattributes InnerClasses, EnclosingMethod

# For debugging
-keepattributes LineNumberTable, SourceFile

# SmileMirror SDK
-keep class com.asus.zenheart.smilemirror.** {
    public *;
}
