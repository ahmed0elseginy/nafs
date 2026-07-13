package com.nafs.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.nafs.app.domain.model.AppLanguage
import com.nafs.app.domain.model.AppSettings
import com.nafs.app.domain.model.ThemeMode
import com.nafs.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.callbackFlow

/**
 * Preferences backed by [SharedPreferences]. Chosen over DataStore because the
 * language must be read synchronously in `Activity.attachBaseContext`, before any
 * coroutine machinery exists.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override val settings: Flow<AppSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override fun currentSettings(): AppSettings = read()

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    override suspend fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    private fun read(): AppSettings = AppSettings(
        themeMode = prefs.getString(KEY_THEME, null).toEnumOr(ThemeMode.SYSTEM),
        language = prefs.getString(KEY_LANGUAGE, null).toLanguageOr(AppLanguage.SYSTEM),
    )

    private fun String?.toEnumOr(default: ThemeMode): ThemeMode =
        this?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: default

    private fun String?.toLanguageOr(default: AppLanguage): AppLanguage =
        this?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: default

    private companion object {
        const val PREFS_NAME = "nafs_settings"
        const val KEY_THEME = "theme_mode"
        const val KEY_LANGUAGE = "language"
    }
}
