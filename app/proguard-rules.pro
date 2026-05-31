# Keep Kotlin serialization classes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep serializable classes and their serializers
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *** companion(...);
    @kotlinx.serialization.Serializable *** $serializer;
}

# Keep serializer fields
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# For Navigation3 and Compose serialization
-keepclassmembers class * implements androidx.navigation3.runtime.NavKey {
    <fields>;
    <init>(...);
}
-keep class * implements androidx.navigation3.runtime.NavKey { *; }
