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
-keep class com.google.genai.** { *; }
-keepclassmembers class com.google.genai.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Ignore compiler / annotation processor metadata packaged in shaded libraries
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.auto.value.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn com.google.errorprone.annotations.**

# Keep Coroutines
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
