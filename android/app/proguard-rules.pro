-keep class com.pixelhaven.beaconrush.quay.LampCask { *; }
-keep class com.pixelhaven.beaconrush.gale.FlarePeg { *; }
-keepclassmembers class com.pixelhaven.beaconrush.gale.FlarePeg {
    <methods>;
}
-keepclasseswithmembernames class * {
    native <methods>;
}

# WorkManager starts via androidx.startup and reflects WorkDatabase_Impl.<init>().
# R8 full mode (AGP 9): `{ *; }` keeps fields/methods but NOT constructors.
-keep class androidx.work.** { *; }
-keep class androidx.work.** {
    <init>(...);
}
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
    protected androidx.room.InvalidationTracker createInvalidationTracker();
    public void clearAllTables();
}
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.InputMerger {
    <init>(...);
}
