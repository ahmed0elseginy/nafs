package com.nafs.app.domain.scoring

import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.model.HabitCheckIn
import com.nafs.app.domain.model.HabitFrequency
import com.nafs.app.domain.model.LifeArea
import com.nafs.app.domain.model.StreakUnit
import com.nafs.app.domain.model.Trend
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreEngineTest {

    private val engine = ScoreEngine(HabitScheduler())

    /** 2026-07-11 is a Saturday; fri is the last day of that Nafs week. */
    private val sat: LocalDate = LocalDate.of(2026, 7, 11)
    private val fri: LocalDate = sat.plusDays(6)

    private fun habit(
        id: Long = 1,
        frequency: HabitFrequency = HabitFrequency.Daily,
        startDate: LocalDate = sat,
        durationDays: Int? = null,
        targetScore: Int = 100,
    ) = Habit(
        id = id,
        areaId = 1,
        name = "habit-$id",
        frequency = frequency,
        startDate = startDate,
        durationDays = durationDays,
        targetScore = targetScore,
    )

    private fun done(date: LocalDate, habitId: Long = 1) =
        HabitCheckIn(habitId = habitId, date = date, completed = true)

    private fun allDays(from: LocalDate, to: LocalDate, habitId: Long = 1): List<HabitCheckIn> =
        generateSequence(from) { if (it < to) it.plusDays(1) else null }
            .map { done(it, habitId) }
            .toList()

    // ---- habit score ----

    @Test
    fun `perfect daily week scores 100`() {
        assertEquals(100, engine.habitScore(habit(), allDays(sat, fri), sat, fri))
    }

    @Test
    fun `partial completion rounds to nearest point`() {
        val checkIns = allDays(sat, sat.plusDays(4)) // 5 of 7
        assertEquals(71, engine.habitScore(habit(), checkIns, sat, fri))
    }

    @Test
    fun `score is null when nothing was expected`() {
        val future = habit(startDate = fri.plusDays(1))
        assertNull(engine.habitScore(future, emptyList(), sat, fri))
    }

    @Test
    fun `weekly overshoot clamps at 100`() {
        val weekly = habit(frequency = HabitFrequency.Weekly(2))
        val checkIns = allDays(sat, fri) // 7 completions against a quota of 2
        assertEquals(100, engine.habitScore(weekly, checkIns, sat, fri))
    }

    // ---- area score ----

    @Test
    fun `area score weights habits by targetScore`() {
        val strong = habit(id = 1, targetScore = 100)
        val weak = habit(id = 2, targetScore = 50)
        val checkIns = allDays(sat, fri, habitId = 1) // habit 2 completed nothing
        // (100*100 + 0*50) / 150 = 67
        assertEquals(67, engine.areaScore(listOf(strong, weak), checkIns, sat, fri))
    }

    @Test
    fun `unscorable habits are excluded, not counted as zero`() {
        val active = habit(id = 1)
        val notStarted = habit(id = 2, startDate = fri.plusDays(1))
        val checkIns = allDays(sat, fri, habitId = 1)
        assertEquals(100, engine.areaScore(listOf(active, notStarted), checkIns, sat, fri))
    }

    @Test
    fun `area score is null when no habit is scorable`() {
        val notStarted = habit(startDate = fri.plusDays(1))
        assertNull(engine.areaScore(listOf(notStarted), emptyList(), sat, fri))
    }

    // ---- overall score ----

    @Test
    fun `overall score weights areas by area weight`() {
        val quran = LifeArea(id = 1, name = "Quran", iconKey = "menu_book", colorArgb = 0, weight = 1.5f)
        val work = LifeArea(id = 2, name = "Work", iconKey = "work", colorArgb = 0, weight = 1f)
        // (80*1.5 + 60*1) / 2.5 = 72
        assertEquals(72, engine.overallScore(listOf(quran to 80, work to 60)))
    }

    @Test
    fun `overall score skips null areas and is null when empty`() {
        val a = LifeArea(id = 1, name = "A", iconKey = "star", colorArgb = 0, weight = 1f)
        val b = LifeArea(id = 2, name = "B", iconKey = "star", colorArgb = 0, weight = 3f)
        assertEquals(80, engine.overallScore(listOf(a to 80, b to null)))
        assertNull(engine.overallScore(listOf(a to null)))
        assertNull(engine.overallScore(emptyList()))
    }

    // ---- trend ----

    @Test
    fun `trend compares last 7 days against the 7 before`() {
        val today = sat.plusDays(13) // Friday of week 2
        val h = habit()
        val strongRecent = allDays(sat.plusDays(7), today) // week 2 perfect, week 1 empty
        assertEquals(Trend.UP, engine.trend(listOf(h), strongRecent, today))

        val strongPast = allDays(sat, sat.plusDays(6)) // week 1 perfect, week 2 empty
        assertEquals(Trend.DOWN, engine.trend(listOf(h), strongPast, today))

        val steady = allDays(sat, today)
        assertEquals(Trend.STEADY, engine.trend(listOf(h), steady, today))
    }

    @Test
    fun `trend is steady when either window is unscorable`() {
        val young = habit(startDate = sat.plusDays(10))
        val today = sat.plusDays(13)
        assertEquals(Trend.STEADY, engine.trend(listOf(young), allDays(sat.plusDays(10), today), today))
    }

    // ---- streaks ----

    @Test
    fun `daily streak counts back and treats an incomplete today as pending`() {
        val today = sat.plusDays(4) // Wednesday
        val checkIns = listOf(done(sat.plusDays(2)), done(sat.plusDays(3))) // Mon, Tue
        val streak = engine.streak(habit(), checkIns, today)
        assertEquals(StreakUnit.DAYS, streak.unit)
        assertEquals(2, streak.current)
    }

    @Test
    fun `daily streak breaks on a missed yesterday`() {
        val today = sat.plusDays(2)
        val checkIns = listOf(done(sat), done(today)) // missed sat+1
        assertEquals(1, engine.streak(habit(), checkIns, today).current)
    }

    @Test
    fun `best streak survives later breaks`() {
        val today = fri // Friday, not completed yet
        val checkIns = listOf(
            done(sat), done(sat.plusDays(1)), done(sat.plusDays(2)), // 3-run
            // Tuesday missed
            done(sat.plusDays(4)), done(sat.plusDays(5)), // 2-run
        )
        val streak = engine.streak(habit(), checkIns, today)
        assertEquals(2, streak.current)
        assertEquals(3, streak.best)
    }

    @Test
    fun `specific-days streak skips unscheduled days`() {
        val h = habit(frequency = HabitFrequency.SpecificDays(setOf(DayOfWeek.SATURDAY, DayOfWeek.MONDAY)))
        val today = sat.plusDays(3) // Tuesday, unscheduled
        val checkIns = listOf(done(sat), done(sat.plusDays(2)))
        assertEquals(2, engine.streak(h, checkIns, today).current)
    }

    @Test
    fun `weekly streak counts quota weeks and ignores the week in progress`() {
        val h = habit(frequency = HabitFrequency.Weekly(2))
        val today = sat.plusDays(20) // Friday of week 3
        val checkIns = listOf(
            done(sat), done(sat.plusDays(1)), // week 1 met
            done(sat.plusDays(7)), done(sat.plusDays(8)), // week 2 met
            done(sat.plusDays(14)), // week 3 in progress: 1 of 2
        )
        val streak = engine.streak(h, checkIns, today)
        assertEquals(StreakUnit.WEEKS, streak.unit)
        assertEquals(2, streak.current)

        val metNow = checkIns + done(sat.plusDays(15))
        assertEquals(3, engine.streak(h, metNow, today).current)
    }

    // ---- detail data ----

    @Test
    fun `last14Days marks scheduling and completion`() {
        val today = sat.plusDays(4)
        val h = habit(startDate = sat) // active for only part of the strip
        val cells = engine.last14Days(h, listOf(done(today)), today)
        assertEquals(14, cells.size)
        assertTrue(cells.last().scheduled)
        assertTrue(cells.last().completed)
        assertTrue(cells.first().date == today.minusDays(13))
        assertTrue(!cells.first().scheduled) // before the habit started
    }

    @Test
    fun `weeklySeries prorates the current week`() {
        val today = sat.plusDays(2) // Monday
        val h = habit(startDate = sat)
        val series = engine.weeklySeries(h, allDays(sat, sat.plusDays(1)), today)
        assertEquals(8, series.size)
        assertNull(series.first().score) // long before the habit existed
        assertEquals(67, series.last().score) // 2 of 3 elapsed days
    }
}
