# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Jan\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Keep HID translation and report engine classes
-keep class com.transcriptor.hid.engine.** { *; }

# Keep Room database entities and DAOs
-keep class com.transcriptor.hid.data.db.** { *; }
-keepclassmembers class * { @androidx.room.* *; }

# Keep domain models
-keep class com.transcriptor.hid.ai.PromptPreset { *; }
-keep class com.transcriptor.hid.service.HostLedState { *; }
-keep class com.transcriptor.hid.service.HidSdpConfiguration { *; }
-keep class com.transcriptor.hid.service.HidQosConfiguration { *; }

# Keep Google GenAI SDK & Kotlinx Serialization
-keepclassmembers class com.google.genai.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Coroutines
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
