-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class ca.machineh3d.lotgddcc.model.**$$serializer { *; }
-keepclassmembers class ca.machineh3d.lotgddcc.model.** {
    *** Companion;
}
-keepclasseswithmembers class ca.machineh3d.lotgddcc.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
