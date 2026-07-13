package com.nafs.app.data.repository

import com.nafs.app.data.local.dao.HabitDao
import com.nafs.app.data.mapper.toDomain
import com.nafs.app.data.mapper.toEntity
import com.nafs.app.di.IoDispatcher
import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.repository.HabitRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val dao: HabitDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : HabitRepository {

    override fun observeHabits(): Flow<List<Habit>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeHabit(id: Long): Flow<Habit?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun upsert(habit: Habit): Long = withContext(io) {
        val rowId = dao.upsert(habit.toEntity())
        if (habit.id != 0L) habit.id else rowId
    }

    override suspend fun delete(id: Long) = withContext(io) {
        dao.deleteById(id)
    }
}
