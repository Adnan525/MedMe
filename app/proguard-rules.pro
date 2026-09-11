# Keep Gson model classes so field names survive reflection-based (de)serialization.
-keep class com.adnaan525.medme.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
