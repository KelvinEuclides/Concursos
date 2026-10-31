# R8/ProGuard rules.

# Jsoup — uses reflection over some internal classes.
-keeppackagenames org.jsoup.nodes
-keep class org.jsoup.** { *; }

# OkHttp / Okio — publisher-provided consumer rules usually suffice; keep quiet.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# MediaPipe Tasks GenAI — JNI + reflection into generated/native classes.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**

# Room — generated *_Impl classes are referenced reflectively.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }

# Kotlin metadata / coroutines internals.
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlin.Metadata { *; }

# androidx.security — EncryptedSharedPreferences and MasterKey use reflection.
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
