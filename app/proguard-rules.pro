# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers and source files for readable stack traces in crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve common reflection and annotation attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# Kotlinx Serialization
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# App Data & Domain Models (Entities, DTOs, State)
-keep class com.pdf.alldocument.office.pdfreaderapp.data.** { *; }
-keep class com.pdf.alldocument.office.pdfreaderapp.domain.model.** { *; }

# allDocsReader module & Office parsing (POI, dom4j)
-keep class com.pdf.alldocument.office.reader.** { *; }
-dontwarn com.pdf.alldocument.office.reader.**
-dontwarn com.pdf.alldocument.office.reader.BR
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class org.dom4j.** { *; }
-dontwarn org.dom4j.**

# PDF Engines (PDFBox & iText)
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# ML Kit Document Scanner & Text Recognition
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.android.gms.vision.**

# Google Generative AI (Gemini) SDK
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Google Play Services, Ads & Billing
-keep class com.google.android.gms.ads.** { *; }
-keep public class com.android.billingclient.** { *; }
-keep class com.google.android.play.core.** { *; }