# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# FastExcel & Woodstox StAX
-keep class org.dhatim.fastexcel.** { *; }
-keep class com.fasterxml.aalto.** { *; }
-dontwarn com.fasterxml.aalto.**
-keep class com.fasterxml.woodstox.** { *; }
-keep class org.codehaus.stax2.** { *; }
-dontwarn com.fasterxml.woodstox.**
-dontwarn org.codehaus.stax2.**
-dontwarn javax.xml.stream.**
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**
