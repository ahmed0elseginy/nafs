package com.nafs.app.domain.model

/**
 * A domain of life the user wants to nurture (e.g. Quran, Health, Work).
 *
 * @property iconKey Key into the app's curated icon set (see `NafsIcons`).
 * @property colorArgb User-chosen ARGB color. This is user data, not theming,
 *   which is why it is stored as a raw value instead of going through the theme.
 * @property weight Relative impact of this area on the overall status score.
 * @property position Manual ordering on the areas screen (insertion order by default).
 */
data class LifeArea(
    val id: Long = 0,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val weight: Float = 1f,
    val position: Int = 0,
)
