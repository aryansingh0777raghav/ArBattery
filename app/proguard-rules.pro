# Proguard rules for ArBattery app
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
