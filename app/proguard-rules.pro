# Keep rules for Minimalist Focus Launcher

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }

# --- Jetpack Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.internal.ComposableLambdaImpl { *; }
-keep class androidx.compose.runtime.internal.ComposableLambdaN { *; }

# --- Room Persistence Library ---
-dontwarn androidx.room.**
-keep class androidx.room.** { *; }
-keep class com.minifocus.launcher.data.entity.** { *; }
-keep class com.minifocus.launcher.data.dao.** { *; }
-keep class com.minifocus.launcher.data.AppDatabase { *; }
-keep class com.minifocus.launcher.data.AppDatabase_Impl { *; }
-keep class * extends androidx.room.RoomDatabase

# --- WorkManager ---
-dontwarn androidx.work.**
-keep class androidx.work.WorkerParameters$RuntimeExtras { *; }
-keep class com.minifocus.launcher.worker.** extends androidx.work.ListenableWorker { *; }

# --- DataStore Preferences ---
-dontwarn androidx.datastore.preferences.core.**

# --- General AndroidX Safety ---
-keep class androidx.lifecycle.viewmodel.CreationExtras { *; }
