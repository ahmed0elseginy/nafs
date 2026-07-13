package com.nafs.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * A recurring practice tied to a [LifeArea].
 *
 * @property durationDays Length of the habit in days counted from [startDate];
 *   null means open-ended.
 * @property targetScore Relative weight of this habit inside its area's score.
 * @property reminderTime Preferred daily reminder time. Stored from Phase 1 so the
 *   schema is ready; notifications are wired up in Phase 4.
 */
data class Habit(
    val id: Long = 0,
    val areaId: Long,
    val name: String,
    val description: String? = null,
    val frequency: HabitFrequency = HabitFrequency.Daily,
    val startDate: LocalDate,
    val durationDays: Int? = null,
    val targetScore: Int = 100,
    val reminderTime: LocalTime? = null,
) {
    /** Last active date, inclusive; null when open-ended. */
    val endDate: LocalDate?
        get() = durationDays?.let { startDate.plusDays(it.toLong() - 1) }

    /** Whether [date] falls inside the habit's active window (ignores the weekday schedule). */
    fun isActiveOn(date: LocalDate): Boolean =
        !date.isBefore(startDate) && (endDate?.let { !date.isAfter(it) } ?: true)
}
