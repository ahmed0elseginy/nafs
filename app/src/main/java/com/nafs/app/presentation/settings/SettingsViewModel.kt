package com.nafs.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafs.app.domain.model.AppLanguage
import com.nafs.app.domain.model.AppSettings
import com.nafs.app.domain.model.ThemeMode
import com.nafs.app.domain.repository.AppDataRepository
import com.nafs.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appDataRepository: AppDataRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settingsRepository.currentSettings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun resetToDemoData() {
        viewModelScope.launch { appDataRepository.resetToDemoData() }
    }

    fun clearAllData() {
        viewModelScope.launch { appDataRepository.clearAllData() }
    }
}
