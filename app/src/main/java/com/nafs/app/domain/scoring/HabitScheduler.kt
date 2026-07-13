package com.nafs.app.domain.scoring

import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.model.HabitCheckIn
import com.nafs.app.domain.model.HabitFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/** The week used across Nafs: Saturday is the first day, Friday the last. */
val WEEK_START: DayOfWeek = DayOfWeek.SATURDAY

/** Start (Saturday) of the week containing this date. */
fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(WEEK_START))

/**
 * Pure schedule arithmetic: which dates a habit is expected on and how many
 * completions a given window demands. No Android, no IO — fully unit-testable.
 */
@Singleton
class HabitScheduler @Inject constructor() {

    /** Intersection of [from]..[to] with the habit's active window; null when empty. */
    fun activeWindow(habit: Habit, from: LocalDate, to: LocalDate): ClosedRange<LocalDate>? {
        val start = maxOf(from, habit.startDate)
        val end = habit.endDate?.let { minOf(to, it) } ?: to
        return if (start > end) null else start..end
    }

    /** Whether the weekday schedule allows [date]. Weekly-quota habits allow any active day. */
    fun isScheduledOn(habit: Habit, date: LocalDate): Boolean {
        if (!habit.isActiveOn(date)) return false
        return when (val frequency = habit.frequency) {
            HabitFrequency.Daily -> true
            is HabitFrequency.Weekly -> true
            is HabitFrequency.SpecificDays -> date.dayOfWeek in frequency.days
        }
    }

    /**
     * Number of completions expected inside [from]..[to].
     *
     * Weekly quotas are pro-rated by active days (floor of `n * days / 7`), so a
     * habit created two days ago is not judged against a full week, and a 30-day
     * window expects `floor(n * 30 / 7)` completions.
     */
    fun expectedCount(habit: Habit, from: LocalDate, to: LocalDate): Int {
        val window = activeWindow(habit, from, to) ?: return 0
        val days = ChronoUnit.DAYS.between(window.start, window.endInclusive).toInt() + 1
        return when (val frequency = habit.frequency) {
            HabitFrequency.Daily -> days
            is HabitFrequency.Weekly -> (frequency.timesPerWeek * days) / 7
            is HabitFrequency.SpecificDays -> countMatchingDays(window, frequency.days)
        }
    }

    /**
     * Completions that count toward the score inside [from]..[to]. Only scheduled
     * days count, and weekly quotas cap at `timesPerWeek` per week so overshooting
     * one week cannot compensate for missing another.
     */
    fun completedCount(
        habit: Habit,
        checkIns: List<HabitCheckIn>,
        from: LocalDate,
        to: LocalDate,
    ): Int {
        val window = activeWindow(habit, from, to) ?: return 0
        val done = checkIns.asSequence()
            .filter { it.habitId == habit.id && it.completed && it.date in window }
            .distinctBy { it.date }
            .toList()
        return when (val frequency = habit.frequency) {
            HabitFrequency.Daily -> done.size
            is HabitFrequency.SpecificDays -> done.count { it.date.dayOfWeek in frequency.days }
            is HabitFrequency.Weekly ->
                done.groupBy { it.date.weekStart() }
                    .values
                    .sumOf { minOf(it.size, frequency.timesPerWeek) }
        }
    }

    /**
     * Whether the habit belongs on the check-in list for [date]. Weekly-quota
     * habits stay listed until the quota is met and remain visible (checked) on
     * a day they were completed.
     */
    fun isDueOn(habit: Habit, date: LocalDate, weekCheckIns: List<HabitCheckIn>): Boolean {
        if (!habit.isActiveOn(date)) return false
        return when (val frequency = habit.frequency) {
            HabitFrequency.Daily -> true
            is HabitFrequency.SpecificDays -> date.dayOfWeek in frequency.days
            is HabitFrequency.Weekly -> {
                val completedOnDate = weekCheckIns.any {
                    it.habitId == habit.id && it.date == date && it.completed
                }
                val completedThisWeek = weekCheckIns.count {
                    it.habitId == habit.id && it.completed && it.date.weekStart() == date.weekStart()
                }
                completedOnDate || completedThisWeek < frequency.timesPerWeek
            }
        }
    }

    private fun countMatchingDays(window: ClosedRange<LocalDate>, days: Set<DayOfWeek>): Int {
        var count = 0
        var date = window.start
        while (!date.isAfter(window.endInclusive)) {
            if (date.dayOfWeek in days) count++
            date = date.plusDays(1)
        }
        return count
    }
}
