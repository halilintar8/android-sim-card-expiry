##############################
# ProGuard / R8 Rules
# android-sim-card-expiry
##############################

# Keep all Room classes and interfaces (Database, DAOs, Entities)
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }

# Keep DAO interfaces annotated with @Dao and their methods
-keepclassmembers class * {
    @androidx.room.Dao <methods>;
}

# Keep model classes fields annotated with Room annotations (ColumnInfo, PrimaryKey)
-keepclassmembers class * {
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.PrimaryKey <fields>;
}

# Keep WorkManager classes and related workers
-keep class androidx.work.** { *; }

# Keep BroadcastReceivers and Services with public constructors
-keep public class * extends android.content.BroadcastReceiver {
    public <init>();
}
-keep public class * extends android.app.Service {
    public <init>();
}

# Keep onReceive methods in BroadcastReceivers
-keepclassmembers class * {
    public void onReceive(android.content.Context, android.content.Intent);
}

# Keep Parcelable implementations including CREATOR fields
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep all enum members (avoid obfuscation of enum constants)
-keepclassmembers enum * {
    *;
}

# Keep custom Application class (replace with your actual application class full name if different)
-keep class **.MyApplication { *; }

# Keep all annotations for reflection
-keepattributes *Annotation*

# Keep line numbers for better crash reporting (optional)
-keepattributes SourceFile,LineNumberTable
