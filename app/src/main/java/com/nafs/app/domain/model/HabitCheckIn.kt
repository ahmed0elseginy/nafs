package com.nafs.app.domain.model

import java.time.LocalDate

/**
 * The record of one habit on one day. At most one check-in exists per
 * habit and date; un-completing keeps the row so the note survives.
 */
data class HabitCheckIn(
    val id: Long = 0,
    val habitId: Long,
    val date: LocalDate,
    val completed: Boolean,
    val note: String? = null,
)
