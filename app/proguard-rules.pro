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
# Firestore ve Serialization için modelleri koru
-keep class com.eyuphanaydin.discbase.Player { *; }
-keep class com.eyuphanaydin.discbase.Match { *; }
-keep class com.eyuphanaydin.discbase.Tournament { *; }
-keep class com.eyuphanaydin.discbase.PointData { *; }
# Veya tüm modelleri tek satırda koru:
-keep class com.eyuphanaydin.discbase.** { *; }

# Kotlinx Serialization sorunlarını önlemek için
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
# 1. Firebase Modellerini Koru (Veritabanı için kritik)
-keep class com.eyuphanaydin.discbase.** { *; }

# 2. Kotlin Serialization (Veri dönüştürme hatası almamak için)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep class kotlinx.serialization.** { *; }

# 3. PDF Oluşturma (Grafik çizimlerinin bozulmaması için)
-keep class android.graphics.** { *; }