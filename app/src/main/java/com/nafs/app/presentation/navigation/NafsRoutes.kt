package com.nafs.app.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

@Serializable
data object HabitsRoute

@Serializable
data object AreasRoute

@Serializable
data class HabitDetailRoute(val habitId: Long)

/** habitId == 0 creates a new habit; areaId != 0 preselects an area. */
@Serializable
data class HabitEditorRoute(val habitId: Long = 0, val areaId: Long = 0)
