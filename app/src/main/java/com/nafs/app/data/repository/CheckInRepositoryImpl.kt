package com.nafs.app.data.repository

import com.nafs.app.data.local.dao.CheckInDao
import com.nafs.app.data.local.entity.HabitCheckInEntity
import com.nafs.app.data.mapper.toDomain
import com.nafs.app.di.IoDispatcher
import com.nafs.app.domain.model.HabitCheckIn
import com.nafs.app.domain.repository.CheckInRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class CheckInRepositoryImpl @Inject constructor(
    private val dao: CheckInDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CheckInRepository {

    override fun observeForDate(date: LocalDate): Flow<List<HabitCheckIn>> =
        dao.observeForDate(date).map { entities -> entities.map { it.toDomain() } }

    override fun observeForHabit(habitId: Long): Flow<List<HabitCheckIn>> =
        dao.observeForHabit(habitId).map { entities -> entities.map { it.toDomain() } }

    override fun observeInRange(from: LocalDate, to: LocalDate): Flow<List<HabitCheckIn>> =
        dao.observeInRange(from, to).map { entities -> entities.map { it.toDomain() } }

    override suspend fun setCompleted(habitId: Long, date: LocalDate, completed: Boolean) =
        withContext(io) {
            val existing = dao.getByHabitAndDate(habitId, date)
            dao.upsert(
                existing?.copy(completed = completed)
                    ?: HabitCheckInEntity(habitId = habitId, date = date, completed = completed, note = null),
            )
        }

    override suspend fun setNote(habitId: Long, date: LocalDate, note: String?) =
        withContext(io) {
            val trimmed = note?.trim()?.takeIf { it.isNotEmpty() }
            val existing = dao.getByHabitAndDate(habitId, date)
            dao.upsert(
                existing?.copy(note = trimmed)
                    ?: HabitCheckInEntity(habitId = habitId, date = date, completed = false, note = trimmed),
            )
        }
}
