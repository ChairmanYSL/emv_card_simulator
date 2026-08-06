# Card Simulator ProGuard Rules
# These rules are specific to the card_simulator application.

# Keep Koin
-keep class org.koin.** { *; }
-keep class * extends org.koin.core.module.Module { *; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.szzt.cardsimulator.**$$serializer { *; }
-keepclassmembers class com.szzt.cardsimulator.** {
    *** Companion;
}
-keepclasseswithmembers class com.szzt.cardsimulator.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities
-keepclassmembers class com.szzt.cardsimulator.profile.impl.db.ProfileEntity {
    *;
}

# Keep NFC HCE
-keep class com.szzt.cardsimulator.hce.impl.CardEmulationService { *; }
