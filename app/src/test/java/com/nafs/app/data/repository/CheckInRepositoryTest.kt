package com.nafs.app.data.repository

import com.nafs.app.data.local.dao.CheckInDao
import com.nafs.app.data.local.entity.HabitCheckInEntity
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeCheckInDao : CheckInDao {

    val items = MutableStateFlow<List<HabitCheckInEntity>>(emptyList())
    private var nextId = 1L

    override fun observeForDate(date: LocalDate): Flow<List<HabitCheckInEntity>> =
        items.map { list -> list.filter { it.date == date } }

    override fun observeForHabit(habitId: Long): Flow<List<HabitCheckInEntity>> =
        items.map { list -> list.filter { it.habitId == habitId }.sortedBy { it.date } }

    override fun observeInRange(from: LocalDate, to: LocalDate): Flow<List<HabitCheckInEntity>> =
        items.map { list -> list.filter { it.date in from..to } }

    override suspend fun getByHabitAndDate(habitId: Long, date: LocalDate): HabitCheckInEntity? =
        items.value.firstOrNull { it.habitId == habitId && it.date == date }

    override suspend fun upsert(checkIn: HabitCheckInEntity) {
        items.update { list ->
            if (checkIn.id != 0L) {
                list.map { if (it.id == checkIn.id) checkIn else it }
            } else {
                list + checkIn.copy(id = nextId++)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CheckInRepositoryTest {

    private val date: LocalDate = LocalDate.of(2026, 7, 11)

    @Test
    fun `setCompleted creates the row when none exists`() = runTest {
        val dao = FakeCheckInDao()
        val repository = CheckInRepositoryImpl(dao, UnconfinedTestDispatcher(testScheduler))

        repository.setCompleted(habitId = 1, date = date, completed = true)

        val checkIns = repository.observeForDate(date).first()
        assertEquals(1, checkIns.size)
        assertTrue(checkIns.single().completed)
        assertNull(checkIns.single().note)
    }

    @Test
    fun `toggling completion preserves the note`() = runTest {
        val dao = FakeCheckInDao()
        val repository = CheckInRepositoryImpl(dao, UnconfinedTestDispatcher(testScheduler))

        repository.setNote(habitId = 1, date = date, note = "felt great")
        repository.setCompleted(habitId = 1, date = date, completed = true)
        repository.setCompleted(habitId = 1, date = date, completed = false)

        val checkIn = repository.observeForHabit(1).first().single()
        assertFalse(checkIn.completed)
        assertEquals("felt great", checkIn.note)
    }

    @Test
    fun `setNote trims text and clears blank notes`() = runTest {
        val dao = FakeCheckInDao()
        val repository = CheckInRepositoryImpl(dao, UnconfinedTestDispatcher(testScheduler))

        repository.setNote(habitId = 1, date = date, note = "  alhamdulillah  ")
        assertEquals("alhamdulillah", repository.observeForHabit(1).first().single().note)

        repository.setNote(habitId = 1, date = date, note = "   ")
        assertNull(repository.observeForHabit(1).first().single().note)
    }

    @Test
    fun `observeInRange returns only dates inside the window`() = runTest {
        val dao = FakeCheckInDao()
        val repository = CheckInRepositoryImpl(dao, UnconfinedTestDispatcher(testScheduler))

        repository.setCompleted(1, date, completed = true)
        repository.setCompleted(1, date.plusDays(5), completed = true)
        repository.setCompleted(1, date.plusDays(10), completed = true)

        val window = repository.observeInRange(date, date.plusDays(6)).first()
        assertEquals(listOf(date, date.plusDays(5)), window.map { it.date })
    }
}
