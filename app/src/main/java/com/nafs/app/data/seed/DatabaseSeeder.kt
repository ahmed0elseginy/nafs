package com.nafs.app.data.seed

import com.nafs.app.data.local.NafsDatabase
import com.nafs.app.data.local.entity.HabitCheckInEntity
import com.nafs.app.data.local.entity.HabitEntity
import com.nafs.app.data.mapper.FrequencyType
import com.nafs.app.data.mapper.encodeDaysMask
import com.nafs.app.domain.scoring.weekStart
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Populates the database with the preset areas, and optionally a set of demo
 * habits with several weeks of generated check-in history.
 *
 * The database is injected lazily via [Provider] to break the dependency cycle
 * (the seeder is wired into the database's own creation callback).
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: Provider<NafsDatabase>,
    private val seedDataProvider: SeedDataProvider,
    private val clock: Clock,
) {

    /** Just the six preset life areas, no habits — used by "start fresh". */
    suspend fun seedAreasOnly() {
        database.get().lifeAreaDao().upsertAll(seedDataProvider.defaultAreas())
    }

    /** Areas plus demo habits and ~[HISTORY_WEEKS] weeks of history. */
    suspend fun seedDemoData() {
        val db = database.get()
        db.lifeAreaDao().upsertAll(seedDataProvider.defaultAreas())
        val areasByPosition = db.lifeAreaDao().getAll().associateBy { it.position }

        val today = LocalDate.now(clock)
        val startDate = today.minusWeeks(HISTORY_WEEKS.toLong()).weekStart()
        // History stops yesterday so today's check-in list is fresh and actionable.
        val lastHistoryDate = today.minusDays(1)
        val totalDays = ChronoUnit.DAYS.between(startDate, lastHistoryDate)
            .coerceAtLeast(1)
            .toDouble()
        val random = Random(SEED)

        for (spec in seedDataProvider.demoHabits()) {
            val area = areasByPosition[spec.areaPosition] ?: continue
            val habitId = db.habitDao().upsert(spec.toEntity(area.id, startDate))
            val history = generateHistory(habitId, spec, startDate, lastHistoryDate, totalDays, random)
            if (history.isNotEmpty()) db.checkInDao().upsertAll(history)
        }
    }

    private fun DemoHabit.toEntity(areaId: Long, startDate: LocalDate) = HabitEntity(
        areaId = areaId,
        name = name,
        description = null,
        frequencyType = frequencyType.name,
        timesPerWeek = timesPerWeek,
        daysMask = if (days.isEmpty()) 0 else encodeDaysMask(days),
        startDate = startDate,
        durationDays = null,
        targetScore = targetScore,
        reminderMinute = null,
    )

    private fun generateHistory(
        habitId: Long,
        spec: DemoHabit,
        start: LocalDate,
        end: LocalDate,
        totalDays: Double,
        random: Random,
    ): List<HabitCheckInEntity> {
        if (end.isBefore(start)) return emptyList()
        return when (spec.frequencyType) {
            FrequencyType.WEEKLY -> weeklyHistory(habitId, spec, start, end, totalDays, random)
            FrequencyType.DAILY,
            FrequencyType.SPECIFIC_DAYS -> dailyHistory(habitId, spec, start, end, totalDays, random)
        }
    }

    private fun dailyHistory(
        habitId: Long,
        spec: DemoHabit,
        start: LocalDate,
        end: LocalDate,
        totalDays: Double,
        random: Random,
    ): List<HabitCheckInEntity> {
        val result = mutableListOf<HabitCheckInEntity>()
        var date = start
        while (!date.isAfter(end)) {
            val scheduled = spec.frequencyType == FrequencyType.DAILY || date.dayOfWeek in spec.days
            if (scheduled && random.nextDouble() < probability(spec, start, date, totalDays)) {
                result += completed(habitId, date)
            }
            date = date.plusDays(1)
        }
        return result
    }

    private fun weeklyHistory(
        habitId: Long,
        spec: DemoHabit,
        start: LocalDate,
        end: LocalDate,
        totalDays: Double,
        random: Random,
    ): List<HabitCheckInEntity> {
        val result = mutableListOf<HabitCheckInEntity>()
        var weekStart = start.weekStart()
        while (!weekStart.isAfter(end)) {
            val weekEnd = minOf(weekStart.plusDays(6), end)
            val activeDays = generateSequence(weekStart) { it.plusDays(1) }
                .takeWhile { !it.isAfter(weekEnd) }
                .toMutableList()
            val p = probability(spec, start, weekStart, totalDays)
            // Usually hits the quota; occasionally falls one short.
            val hit = if (random.nextDouble() < p) spec.timesPerWeek else spec.timesPerWeek - 1
            val count = hit.coerceIn(0, activeDays.size)
            activeDays.shuffle(random)
            activeDays.take(count).forEach { result += completed(habitId, it) }
            weekStart = weekStart.plusWeeks(1)
        }
        return result
    }

    /** Baseline bias plus a gentle upward drift toward the present. */
    private fun probability(spec: DemoHabit, start: LocalDate, date: LocalDate, totalDays: Double): Double {
        val progress = ChronoUnit.DAYS.between(start, date) / totalDays
        return (spec.completionBias + DRIFT * progress).coerceIn(0.05, 0.98)
    }

    private fun completed(habitId: Long, date: LocalDate) =
        HabitCheckInEntity(habitId = habitId, date = date, completed = true, note = null)

    private companion object {
        const val HISTORY_WEEKS = 10
        const val DRIFT = 0.22
        const val SEED = 20260713L
    }
}
