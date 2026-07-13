package com.nafs.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Calm teal-on-paper palette. Dynamic color is intentionally off so the app
// keeps its reflective, consistent character.

val PrimaryLight = Color(0xFF00695C)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFB8EAE0)
val OnPrimaryContainerLight = Color(0xFF00201C)
val SecondaryLight = Color(0xFF4A635E)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFCDE8E1)
val OnSecondaryContainerLight = Color(0xFF06201B)
val TertiaryLight = Color(0xFF5B5B7E)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFE2DFFF)
val OnTertiaryContainerLight = Color(0xFF181837)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val BackgroundLight = Color(0xFFF7FAF8)
val OnBackgroundLight = Color(0xFF191C1B)
val SurfaceLight = Color(0xFFF7FAF8)
val OnSurfaceLight = Color(0xFF191C1B)
val SurfaceVariantLight = Color(0xFFDBE5E1)
val OnSurfaceVariantLight = Color(0xFF3F4946)
val OutlineLight = Color(0xFF6F7976)
val OutlineVariantLight = Color(0xFFBFC9C5)

val PrimaryDark = Color(0xFF7FD6C7)
val OnPrimaryDark = Color(0xFF003731)
val PrimaryContainerDark = Color(0xFF005046)
val OnPrimaryContainerDark = Color(0xFF9CF2E2)
val SecondaryDark = Color(0xFFB1CCC5)
val OnSecondaryDark = Color(0xFF1C3530)
val SecondaryContainerDark = Color(0xFF334B46)
val OnSecondaryContainerDark = Color(0xFFCDE8E1)
val TertiaryDark = Color(0xFFC4C3EB)
val OnTertiaryDark = Color(0xFF2D2D4D)
val TertiaryContainerDark = Color(0xFF434465)
val OnTertiaryContainerDark = Color(0xFFE2DFFF)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
val BackgroundDark = Color(0xFF101413)
val OnBackgroundDark = Color(0xFFDFE4E1)
val SurfaceDark = Color(0xFF101413)
val OnSurfaceDark = Color(0xFFDFE4E1)
val SurfaceVariantDark = Color(0xFF3F4946)
val OnSurfaceVariantDark = Color(0xFFBFC9C5)
val OutlineDark = Color(0xFF89938F)
val OutlineVariantDark = Color(0xFF3F4946)

/**
 * Curated ARGB values users can assign to life areas. Area colors are user
 * data (persisted per area), which is why they bypass the Material theme.
 */
val AreaColorOptions: List<Long> = listOf(
    0xFF2E7D57, // green
    0xFF00695C, // teal
    0xFF00838F, // cyan
    0xFF3949AB, // indigo
    0xFF5C6BC0, // periwinkle
    0xFF7B5EA7, // plum
    0xFFAD1457, // berry
    0xFFB3542E, // terracotta
    0xFF9A7B0A, // olive gold
    0xFF6D4C41, // brown
    0xFF546E7A, // blue grey
    0xFF33691E, // moss
)
