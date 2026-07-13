package com.nafs.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nafs.app.data.local.entity.HabitCheckInEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {

    @Query("SELECT * FROM habit_check_ins WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<List<HabitCheckInEntity>>

    @Query("SELECT * FROM habit_check_ins WHERE habit_id = :habitId ORDER BY date")
    fun observeForHabit(habitId: Long): Flow<List<HabitCheckInEntity>>

    @Query("SELECT * FROM habit_check_ins WHERE date BETWEEN :from AND :to")
    fun observeInRange(from: LocalDate, to: LocalDate): Flow<List<HabitCheckInEntity>>

    @Query("SELECT * FROM habit_check_ins WHERE habit_id = :habitId AND date = :date LIMIT 1")
    suspend fun getByHabitAndDate(habitId: Long, date: LocalDate): HabitCheckInEntity?

    @Upsert
    suspend fun upsert(checkIn: HabitCheckInEntity)

    @Upsert
    suspend fun upsertAll(checkIns: List<HabitCheckInEntity>)
}
