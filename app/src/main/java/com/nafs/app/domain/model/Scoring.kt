package com.nafs.app.domain.model

import java.time.LocalDate

/** Direction a score moved between the last 7 days and the 7 before them. */
enum class Trend { UP, DOWN, STEADY }

/** Unit a streak is counted in — weekly-quota habits streak by weeks, others by days. */
enum class StreakUnit { DAYS, WEEKS }

data class StreakInfo(
    val current: Int,
    val best: Int,
    val unit: StreakUnit,
)

/** One cell of the recent-history strip on the habit detail screen. */
data class DayCell(
    val date: LocalDate,
    val scheduled: Boolean,
    val completed: Boolean,
)

/** Completion score of one Saturday-to-Friday week; null when nothing was expected. */
data class WeekPoint(
    val weekStart: LocalDate,
    val score: Int?,
)
