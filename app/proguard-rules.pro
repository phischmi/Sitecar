# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class **$$serializer {
    *** descriptor;
}
-keepclasseswithmembers class app.sitecar.client.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# PdfBox-Android — verwendet Reflection für Font- und Resource-Loading
-keep class org.apache.pdfbox.** { *; }
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-dontwarn org.apache.pdfbox.**
-dontwarn com.tom_roush.pdfbox.**
-dontwarn javax.**
-dontwarn org.bouncycastle.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
