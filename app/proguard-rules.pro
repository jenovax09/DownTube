# Keep model classes used by kotlinx.serialization and Room.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.snaptube.clone.core.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.snaptube.clone.core.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room generated code
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }

# OkHttp / Jsoup
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.jsoup.**

# Media3
-dontwarn androidx.media3.**
