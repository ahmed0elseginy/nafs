package com.nafs.app.domain.model

import java.time.DayOfWeek

/** How often a habit is expected to be performed. */
sealed interface HabitFrequency {

    /** Every day of the active window. */
    data object Daily : HabitFrequency

    /**
     * [timesPerWeek] completions per week, on any days the user chooses.
     * Weeks run Saturday through Friday everywhere in Nafs.
     */
    data class Weekly(val timesPerWeek: Int) : HabitFrequency

    /** Only on the given weekdays (e.g. gym on Saturday, Monday, Wednesday). */
    data class SpecificDays(val days: Set<DayOfWeek>) : HabitFrequency
}
