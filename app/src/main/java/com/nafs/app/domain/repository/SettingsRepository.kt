package com.nafs.app.domain.repository

import com.nafs.app.domain.model.AppLanguage
import com.nafs.app.domain.model.AppSettings
import com.nafs.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    /** Preferences as a reactive stream, emitting the current value immediately. */
    val settings: Flow<AppSettings>

    /** Synchronous snapshot, used before Compose is available (e.g. locale at attach time). */
    fun currentSettings(): AppSettings

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setLanguage(language: AppLanguage)
}
