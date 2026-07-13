package com.nafs.app.domain.repository

import com.nafs.app.domain.model.HabitCheckIn
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface CheckInRepository {

    fun observeForDate(date: LocalDate): Flow<List<HabitCheckIn>>

    fun observeForHabit(habitId: Long): Flow<List<HabitCheckIn>>

    /** All check-ins with dates in [from]..[to], across habits. */
    fun observeInRange(from: LocalDate, to: LocalDate): Flow<List<HabitCheckIn>>

    /** Marks the habit (un)completed for the date, preserving any existing note. */
    suspend fun setCompleted(habitId: Long, date: LocalDate, completed: Boolean)

    /** Sets or clears the note for the date, creating the check-in row if needed. */
    suspend fun setNote(habitId: Long, date: LocalDate, note: String?)
}
