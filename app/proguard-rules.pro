# Add project specific ProGuard rules here.
-keep class com.duallayersecurity.app.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
