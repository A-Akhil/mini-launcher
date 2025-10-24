package com.minifocus.launcher.data.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.launcherSettingsDataStore by preferencesDataStore(name = "launcher_settings")
