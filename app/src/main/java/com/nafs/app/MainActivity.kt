package com.nafs.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nafs.app.di.SettingsEntryPoint
import com.nafs.app.domain.model.AppLanguage
import com.nafs.app.domain.model.ThemeMode
import com.nafs.app.domain.repository.SettingsRepository
import com.nafs.app.presentation.navigation.NafsApp
import com.nafs.app.presentation.theme.NafsTheme
import com.nafs.app.util.LocaleManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

/** Single-activity host; all screens live in the Compose navigation graph. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    /** Language baked into this activity instance; a change triggers [recreate]. */
    private var appliedLanguage: AppLanguage = AppLanguage.SYSTEM

    override fun attachBaseContext(newBase: Context) {
        val settings = EntryPointAccessors
            .fromApplication(newBase.applicationContext, SettingsEntryPoint::class.java)
            .settingsRepository()
            .currentSettings()
        appliedLanguage = settings.language
        super.attachBaseContext(LocaleManager.wrap(newBase, settings.language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = settingsRepository.currentSettings())

            LaunchedEffect(settings.language) {
                if (settings.language != appliedLanguage) recreate()
            }

            val darkTheme = when (settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            NafsTheme(darkTheme = darkTheme) {
                NafsApp()
            }
        }
    }
}
