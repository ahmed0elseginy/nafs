package com.nafs.app.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Curated icon set for life areas. Icons are persisted by [key], so keys are
 * append-only: never rename or remove one that may exist in a user's database.
 */
object NafsIcons {

    val options: List<Pair<String, ImageVector>> = listOf(
        "menu_book" to Icons.AutoMirrored.Rounded.MenuBook,
        "mosque" to Icons.Rounded.Mosque,
        "school" to Icons.Rounded.School,
        "psychology" to Icons.Rounded.Psychology,
        "monitor_heart" to Icons.Rounded.MonitorHeart,
        "favorite" to Icons.Rounded.Favorite,
        "fitness_center" to Icons.Rounded.FitnessCenter,
        "directions_run" to Icons.AutoMirrored.Rounded.DirectionsRun,
        "self_improvement" to Icons.Rounded.SelfImprovement,
        "spa" to Icons.Rounded.Spa,
        "translate" to Icons.Rounded.Translate,
        "work" to Icons.Rounded.Work,
        "code" to Icons.Rounded.Code,
        "savings" to Icons.Rounded.Savings,
        "restaurant" to Icons.Rounded.Restaurant,
        "water_drop" to Icons.Rounded.WaterDrop,
        "bedtime" to Icons.Rounded.Bedtime,
        "groups" to Icons.Rounded.Groups,
        "brush" to Icons.Rounded.Brush,
        "timer" to Icons.Rounded.Timer,
        "star" to Icons.Rounded.Star,
    )

    private val byKey: Map<String, ImageVector> = options.toMap()

    /** Falls back to a star for unknown keys so old data never crashes the UI. */
    fun forKey(key: String): ImageVector = byKey[key] ?: Icons.Rounded.Star
}
