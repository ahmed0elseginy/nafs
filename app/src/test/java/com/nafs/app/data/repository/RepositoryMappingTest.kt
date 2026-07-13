package com.nafs.app.data.repository

import com.nafs.app.data.local.dao.HabitDao
import com.nafs.app.data.local.dao.LifeAreaDao
import com.nafs.app.data.local.entity.HabitEntity
import com.nafs.app.data.local.entity.LifeAreaEntity
import com.nafs.app.data.mapper.decodeDaysMask
import com.nafs.app.data.mapper.encodeDaysMask
import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.model.HabitFrequency
import com.nafs.app.domain.model.LifeArea
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeHabitDao : HabitDao {

    val items = MutableStateFlow<List<HabitEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<HabitEntity>> = items.map { it.sortedBy(HabitEntity::id) }

    override fun observeById(id: Long): Flow<HabitEntity?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun upsert(habit: HabitEntity): Long =
        if (habit.id != 0L) {
            items.update { list -> list.map { if (it.id == habit.id) habit else it } }
            -1L // Room's @Upsert returns -1 for updates
        } else {
            val id = nextId++
            items.update { it + habit.copy(id = id) }
            id
        }

    override suspend fun deleteById(id: Long) {
        items.update { list -> list.filterNot { it.id == id } }
    }
}

private class FakeLifeAreaDao : LifeAreaDao {

    val items = MutableStateFlow<List<LifeAreaEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<LifeAreaEntity>> =
        items.map { it.sortedWith(compareBy(LifeAreaEntity::position, LifeAreaEntity::id)) }

    override fun observeById(id: Long): Flow<LifeAreaEntity?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun upsert(area: LifeAreaEntity): Long =
        if (area.id != 0L) {
            items.update { list -> list.map { if (it.id == area.id) area else it } }
            -1L
        } else {
            val id = nextId++
            items.update { it + area.copy(id = id) }
            id
        }

    override suspend fun upsertAll(areas: List<LifeAreaEntity>) {
        areas.forEach { upsert(it) }
    }

    override suspend fun deleteById(id: Long) {
        items.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun count(): Int = items.value.size
}

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryMappingTest {

    private val start: LocalDate = LocalDate.of(2026, 7, 11)

    private fun habit(frequency: HabitFrequency) = Habit(
        areaId = 1,
        name = "Quran review",
        description = "Two pages after Fajr",
        frequency = frequency,
        startDate = start,
        durationDays = 60,
        targetScore = 80,
        reminderTime = LocalTime.of(20, 30),
    )

    @Test
    fun `days mask round-trips every weekday combination edge`() {
        assertEquals(setOf(DayOfWeek.MONDAY), decodeDaysMask(encodeDaysMask(setOf(DayOfWeek.MONDAY))))
        assertEquals(setOf(DayOfWeek.SUNDAY), decodeDaysMask(encodeDaysMask(setOf(DayOfWeek.SUNDAY))))
        assertEquals(DayOfWeek.entries.toSet(), decodeDaysMask(encodeDaysMask(DayOfWeek.entries.toSet())))
        assertEquals(emptySet<DayOfWeek>(), decodeDaysMask(0))
    }

    @Test
    fun `habit round-trips through the repository for every frequency type`() = runTest {
        val repository = HabitRepositoryImpl(FakeHabitDao(), UnconfinedTestDispatcher(testScheduler))
        val frequencies = listOf(
            HabitFrequency.Daily,
            HabitFrequency.Weekly(3),
            HabitFrequency.SpecificDays(setOf(DayOfWeek.SATURDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)),
        )

        for (frequency in frequencies) {
            val id = repository.upsert(habit(frequency))
            val loaded = repository.observeHabit(id).first()!!
            assertEquals(frequency, loaded.frequency)
            assertEquals("Quran review", loaded.name)
            assertEquals("Two pages after Fajr", loaded.description)
            assertEquals(start, loaded.startDate)
            assertEquals(60, loaded.durationDays)
            assertEquals(start.plusDays(59), loaded.endDate)
            assertEquals(80, loaded.targetScore)
            assertEquals(LocalTime.of(20, 30), loaded.reminderTime)
        }
    }

    @Test
    fun `upserting an existing habit keeps its id`() = runTest {
        val repository = HabitRepositoryImpl(FakeHabitDao(), UnconfinedTestDispatcher(testScheduler))

        val id = repository.upsert(habit(HabitFrequency.Daily))
        val renamed = repository.observeHabit(id).first()!!.copy(name = "Renamed")
        val returnedId = repository.upsert(renamed)

        assertEquals(id, returnedId)
        assertEquals("Renamed", repository.observeHabit(id).first()!!.name)
        assertEquals(1, repository.observeHabits().first().size)
    }

    @Test
    fun `new life areas are appended at the end`() = runTest {
        val repository = LifeAreaRepositoryImpl(FakeLifeAreaDao(), UnconfinedTestDispatcher(testScheduler))

        repository.upsert(LifeArea(name = "Quran", iconKey = "menu_book", colorArgb = 0xFF2E7D57))
        repository.upsert(LifeArea(name = "Health", iconKey = "monitor_heart", colorArgb = 0xFFAD1457))

        val areas = repository.observeAreas().first()
        assertEquals(listOf("Quran", "Health"), areas.map(LifeArea::name))
        assertEquals(listOf(0, 1), areas.map(LifeArea::position))
    }

    @Test
    fun `updating an area keeps id and position`() = runTest {
        val repository = LifeAreaRepositoryImpl(FakeLifeAreaDao(), UnconfinedTestDispatcher(testScheduler))

        val id = repository.upsert(LifeArea(name = "Study", iconKey = "school", colorArgb = 0xFF3949AB))
        val updated = repository.observeArea(id).first()!!.copy(name = "Deep study", weight = 2f)
        assertEquals(id, repository.upsert(updated))

        val loaded = repository.observeArea(id).first()!!
        assertEquals("Deep study", loaded.name)
        assertEquals(2f, loaded.weight)
        assertEquals(0, loaded.position)
    }
}
