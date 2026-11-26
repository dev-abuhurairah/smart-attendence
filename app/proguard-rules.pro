# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keepclassmembers class com.smartattendance.app.data.local.db.dao.** { *; }
-keepclassmembers class com.smartattendance.app.domain.model.** { *; }

# FastExcel & Woodstox StAX
-keep class org.dhatim.fastexcel.** { *; }
-keep class com.fasterxml.aalto.** { *; }
-dontwarn com.fasterxml.aalto.**
-keep class com.fasterxml.woodstox.** { *; }
-keep class com.ctc.wstx.** { *; }
-dontwarn com.ctc.wstx.**
-dontwarn aQute.bnd.**
-dontwarn aQute.**
-keep class org.codehaus.stax2.** { *; }
-dontwarn com.fasterxml.woodstox.**
-dontwarn org.codehaus.stax2.**
-dontwarn javax.xml.stream.**
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# Coroutines & Lifecycle
-keep class * extends androidx.lifecycle.ViewModel { *; }

# R8 Optimization: Strip debug logs in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
