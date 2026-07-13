package com.nafs.app.di

import com.nafs.app.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Lets [com.nafs.app.MainActivity.attachBaseContext] reach the settings before
 * Hilt field injection has run, so the saved language can be applied to the very
 * first configuration.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryPoint {
    fun settingsRepository(): SettingsRepository
}
