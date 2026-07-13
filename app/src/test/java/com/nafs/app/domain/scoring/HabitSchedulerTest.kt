package com.nafs.app.domain.scoring

import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.model.HabitCheckIn
import com.nafs.app.domain.model.HabitFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitSchedulerTest {

    private val scheduler = HabitScheduler()

    /** 2026-07-11 is a Saturday — the anchor for all week arithmetic below. */
    private val sat: LocalDate = LocalDate.of(2026, 7, 11)

    private fun habit(
        frequency: HabitFrequency,
        startDate: LocalDate = sat.minusDays(60),
        durationDays: Int? = null,
    ) = Habit(id = 1, areaId = 1, name = "habit", frequency = frequency, startDate = startDate, durationDays = durationDays)

    private fun done(date: LocalDate) = HabitCheckIn(habitId = 1, date = date, completed = true)

    @Test
    fun `anchor date is a saturday`() {
        assertEquals(DayOfWeek.SATURDAY, sat.dayOfWeek)
    }

    @Test
    fun `weekStart snaps to the previous or same saturday`() {
        assertEquals(sat, sat.weekStart())
        assertEquals(sat, sat.plusDays(6).weekStart()) // Friday belongs to the same week
        assertEquals(sat.minusDays(7), sat.minusDays(1).weekStart())
    }

    @Test
    fun `daily habit expects one completion per active day`() {
        assertEquals(7, scheduler.expectedCount(habit(HabitFrequency.Daily), sat, sat.plusDays(6)))
    }

    @Test
    fun `expected count is truncated to the habit start date`() {
        val h = habit(HabitFrequency.Daily, startDate = sat.plusDays(3))
        assertEquals(4, scheduler.expectedCount(h, sat, sat.plusDays(6)))
    }

    @Test
    fun `expected count is truncated by duration`() {
        val h = habit(HabitFrequency.Daily, startDate = sat, durationDays = 3)
        assertEquals(3, scheduler.expectedCount(h, sat, sat.plusDays(6)))
    }

    @Test
    fun `specific days count only matching weekdays`() {
        val h = habit(HabitFrequency.SpecificDays(setOf(DayOfWeek.SATURDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
        assertEquals(3, scheduler.expectedCount(h, sat, sat.plusDays(6)))
        // Window starting Sunday drops the Saturday occurrence.
        assertEquals(2, scheduler.expectedCount(h, sat.plusDays(1), sat.plusDays(6)))
    }

    @Test
    fun `weekly quota is prorated by active days`() {
        val h = habit(HabitFrequency.Weekly(3))
        assertEquals(12, scheduler.expectedCount(h, sat, sat.plusDays(29))) // floor(3 * 30 / 7)
        assertEquals(3, scheduler.expectedCount(h, sat, sat.plusDays(6)))
        assertEquals(0, scheduler.expectedCount(h, sat, sat.plusDays(1))) // floor(3 * 2 / 7)
    }

    @Test
    fun `weekly completions cap at the quota per week`() {
        val h = habit(HabitFrequency.Weekly(2))
        val checkIns = listOf(
            done(sat), done(sat.plusDays(1)), done(sat.plusDays(2)), done(sat.plusDays(3)), // 4 in week 1
            done(sat.plusDays(7)), // 1 in week 2
        )
        assertEquals(3, scheduler.completedCount(h, checkIns, sat, sat.plusDays(13)))
    }

    @Test
    fun `completions on unscheduled days do not count`() {
        val h = habit(HabitFrequency.SpecificDays(setOf(DayOfWeek.SATURDAY)))
        val checkIns = listOf(done(sat.plusDays(1))) // Sunday
        assertEquals(0, scheduler.completedCount(h, checkIns, sat, sat.plusDays(6)))
    }

    @Test
    fun `incomplete check-ins do not count`() {
        val h = habit(HabitFrequency.Daily)
        val checkIns = listOf(HabitCheckIn(habitId = 1, date = sat, completed = false, note = "tried"))
        assertEquals(0, scheduler.completedCount(h, checkIns, sat, sat))
    }

    @Test
    fun `weekly habit is due until the quota is met`() {
        val h = habit(HabitFrequency.Weekly(2))
        val week = listOf(done(sat), done(sat.plusDays(1)))
        // Quota met on Monday: no longer due.
        assertFalse(scheduler.isDueOn(h, sat.plusDays(2), week))
        // But it stays visible (checked) on a day it was completed.
        assertTrue(scheduler.isDueOn(h, sat.plusDays(1), week))
        // Under quota it is due any day.
        assertTrue(scheduler.isDueOn(h, sat.plusDays(2), listOf(done(sat))))
    }

    @Test
    fun `specific-days habit is due only on its days`() {
        val h = habit(HabitFrequency.SpecificDays(setOf(DayOfWeek.MONDAY)))
        assertTrue(scheduler.isDueOn(h, sat.plusDays(2), emptyList())) // Monday
        assertFalse(scheduler.isDueOn(h, sat.plusDays(3), emptyList())) // Tuesday
    }

    @Test
    fun `habit is not due outside its active window`() {
        val notStarted = habit(HabitFrequency.Daily, startDate = sat.plusDays(1))
        assertFalse(scheduler.isDueOn(notStarted, sat, emptyList()))

        val ended = habit(HabitFrequency.Daily, startDate = sat, durationDays = 1)
        assertTrue(scheduler.isScheduledOn(ended, sat))
        assertFalse(scheduler.isScheduledOn(ended, sat.plusDays(1)))
    }
}
