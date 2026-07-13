package com.nafs.app.domain.model

/** How the app decides between the light and dark color scheme. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Language the UI is rendered in. [SYSTEM] follows the device locale; the others
 * force a specific locale via a per-app configuration override.
 *
 * @property tag BCP-47 tag applied to the configuration, or null to follow the system.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ARABIC("ar"),
}

/** User preferences that live outside the Room database. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
)
