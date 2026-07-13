package com.nafs.app.domain.repository

import com.nafs.app.domain.model.Habit
import kotlinx.coroutines.flow.Flow

interface HabitRepository {

    /** All habits in insertion order. */
    fun observeHabits(): Flow<List<Habit>>

    fun observeHabit(id: Long): Flow<Habit?>

    /** Inserts (id == 0) or updates; returns the persisted id. */
    suspend fun upsert(habit: Habit): Long

    /** Deletes the habit; its check-ins cascade. */
    suspend fun delete(id: Long)
}
