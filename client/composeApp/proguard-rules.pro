# Keep Kotlin serialization and Ktor models for release builds (Итерация 9).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class ru.vertical.climbing.**$$serializer { *; }
-keepclassmembers class ru.vertical.climbing.** {
    *** Companion;
}
-keepclasseswithmembers class ru.vertical.climbing.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }

-keep class com.arkivanov.** { *; }
