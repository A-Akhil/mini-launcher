package com.minifocus.launcher

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

object LocaleUtils {
    fun getLocalizedContext(context: Context): Context {
        val languageTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager?.applicationLocales?.toLanguageTags()?.split(",")?.firstOrNull()?.trim().orEmpty()
        } else {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
                .split(",")
                .firstOrNull()
                ?.trim()
                .orEmpty()
        }

        if (languageTag.isNotEmpty()) {
            val conf = Configuration(context.resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                conf.setLocales(LocaleList.forLanguageTags(languageTag))
            } else {
                @Suppress("DEPRECATION")
                conf.locale = java.util.Locale.forLanguageTag(languageTag)
            }
            return context.createConfigurationContext(conf)
        }
        return context
    }
}
