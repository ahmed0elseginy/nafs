package com.nafs.app.domain.scoring

import com.nafs.app.domain.model.DayCell
import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.model.HabitCheckIn
import com.nafs.app.domain.model.HabitFrequency
import com.nafs.app.domain.model.LifeArea
import com.nafs.app.domain.model.StreakInfo
import com.nafs.app.domain.model.StreakUnit
import com.nafs.app.domain.model.Trend
import com.nafs.app.domain.model.WeekPoint
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Pure scoring rules: check-ins → habit score → area score → overall status.
 *
 * All scores use a rolling window (30 days by convention, see [scoreWindowStart])
 * so old misses age out. A habit with nothing expected in the window scores
 * `null` and is excluded from aggregation rather than counted as zero.
 */
@Singleton
class ScoreEngine @Inject constructor(private val scheduler: HabitScheduler) {

    /** Habit score 0..100 inside the window, or null when nothing was expected. */
    fun habitScore(habit: Habit, checkIns: List<HabitCheckIn>, from: LocalDate, to: LocalDate): Int? {
        val expected = scheduler.expectedCount(habit, from, to)
        if (expected == 0) return null
        val completed = scheduler.completedCount(habit, checkIns, from, to)
        return ((100.0 * completed) / expected).roundToInt().coerceIn(0, 100)
    }

    /**
     * Score of a set of habits (typically one area), each habit weighted by its
     * `targetScore`. Null when no habit in the set is scorable.
     */
    fun areaScore(habits: List<Habit>, checkIns: List<HabitCheckIn>, from: LocalDate, to: LocalDate): Int? {
        var numerator = 0.0
        var denominator = 0.0
        for (habit in habits) {
            val score = habitScore(habit, checkIns, from, to) ?: continue
            val weight = habit.targetScore.coerceAtLeast(1)
            numerator += score.toDouble() * weight
            denominator += weight.toDouble()
        }
        return if (denominator == 0.0) null else (numerator / denominator).roundToInt()
    }

    /** Overall status: area scores weighted by each area's `weight`. */
    fun overallScore(areaScores: List<Pair<LifeArea, Int?>>): Int? {
        var numerator = 0.0
        var denominator = 0.0
        for ((area, score) in areaScores) {
            if (score == null) continue
            val weight = area.weight.coerceAtLeast(0.01f)
            numerator += score.toDouble() * weight
            denominator += weight.toDouble()
        }
        return if (denominator == 0.0) null else (numerator / denominator).roundToInt()
    }

    /**
     * Overall status across every area for the window [from]..[to]. Each area is
     * scored from the habits belonging to it, then combined by area weight.
     */
    fun overallScoreAt(
        areas: List<LifeArea>,
        habits: List<Habit>,
        checkIns: List<HabitCheckIn>,
        from: LocalDate,
        to: LocalDate,
    ): Int? {
        val habitsByArea = habits.groupBy { it.areaId }
        val areaScores = areas.map { area ->
            area to areaScore(habitsByArea[area.id].orEmpty(), checkIns, from, to)
        }
        return overallScore(areaScores)
    }

    /** Per-week overall status for the Overall tab chart, oldest first. Current week is pro-rated. */
    fun overallWeeklySeries(
        areas: List<LifeArea>,
        habits: List<Habit>,
        checkIns: List<HabitCheckIn>,
        today: LocalDate,
        weeks: Int = 10,
    ): List<WeekPoint> {
        val currentWeek = today.weekStart()
        return (weeks - 1 downTo 0).map { index ->
            val start = currentWeek.minusWeeks(index.toLong())
            val end = minOf(start.plusDays(6), today)
            WeekPoint(start, overallScoreAt(areas, habits, checkIns, start, end))
        }
    }

    /** Overall trend: last 7 days versus the 7 before them. STEADY when either window is unscorable. */
    fun overallTrend(
        areas: List<LifeArea>,
        habits: List<Habit>,
        checkIns: List<HabitCheckIn>,
        today: LocalDate,
    ): Trend {
        val recent = overallScoreAt(areas, habits, checkIns, today.minusDays(6), today)
        val previous = overallScoreAt(areas, habits, checkIns, today.minusDays(13), today.minusDays(7))
        if (recent == null || previous == null) return Trend.STEADY
        return when {
            recent - previous > TREND_THRESHOLD -> Trend.UP
            previous - recent > TREND_THRESHOLD -> Trend.DOWN
            else -> Trend.STEADY
        }
    }

    /**
     * Trend of the last 7 days versus the 7 days before them, for any habit set
     * (a whole area or a single habit). STEADY when either window is unscorable.
     */
    fun trend(habits: List<Habit>, checkIns: List<HabitCheckIn>, today: LocalDate): Trend {
        val recent = areaScore(habits, checkIns, today.minusDays(6), today)
        val previous = areaScore(habits, checkIns, today.minusDays(13), today.minusDays(7))
        if (recent == null || previous == null) return Trend.STEADY
        return when {
            recent - previous > TREND_THRESHOLD -> Trend.UP
            previous - recent > TREND_THRESHOLD -> Trend.DOWN
            else -> Trend.STEADY
        }
    }

    /**
     * Current and best streak. Daily and specific-day habits streak over scheduled
     * days; weekly-quota habits streak over weeks that met the quota. An
     * incomplete "today" (or in-progress week) is pending — it neither extends
     * nor breaks the streak.
     */
    fun streak(habit: Habit, checkIns: List<HabitCheckIn>, today: LocalDate): StreakInfo =
        when (val frequency = habit.frequency) {
            is HabitFrequency.Weekly -> weekStreak(habit, frequency.timesPerWeek, checkIns, today)
            else -> dayStreak(habit, checkIns, today)
        }

    /** The 14-day strip shown on the habit detail screen, oldest first. */
    fun last14Days(habit: Habit, checkIns: List<HabitCheckIn>, today: LocalDate): List<DayCell> {
        val completedDates = completedDates(habit, checkIns)
        return (13 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DayCell(
                date = date,
                scheduled = scheduler.isScheduledOn(habit, date),
                completed = date in completedDates,
            )
        }
    }

    /** Per-week scores for the detail chart, oldest first. Current week is pro-rated. */
    fun weeklySeries(
        habit: Habit,
        checkIns: List<HabitCheckIn>,
        today: LocalDate,
        weeks: Int = 8,
    ): List<WeekPoint> {
        val currentWeek = today.weekStart()
        return (weeks - 1 downTo 0).map { index ->
            val start = currentWeek.minusWeeks(index.toLong())
            val end = minOf(start.plusDays(6), today)
            WeekPoint(start, habitScore(habit, checkIns, start, end))
        }
    }

    /** Start of the standard rolling score window ending at [today]. */
    fun scoreWindowStart(today: LocalDate): LocalDate = today.minusDays(SCORE_WINDOW_DAYS - 1)

    private fun dayStreak(habit: Habit, checkIns: List<HabitCheckIn>, today: LocalDate): StreakInfo {
        val completedDates = completedDates(habit, checkIns)
        val lastActive = minOf(today, habit.endDate ?: today)
        val firstConsidered = maxOf(habit.startDate, today.minusDays(MAX_LOOKBACK_DAYS))

        var current = 0
        var date = lastActive
        while (!date.isBefore(firstConsidered)) {
            if (scheduler.isScheduledOn(habit, date)) {
                when {
                    date in completedDates -> current++
                    date == today -> Unit // pending, not a break
                    else -> break
                }
            }
            date = date.minusDays(1)
        }

        var best = 0
        var run = 0
        date = firstConsidered
        while (!date.isAfter(lastActive)) {
            if (scheduler.isScheduledOn(habit, date)) {
                when {
                    date in completedDates -> {
                        run++
                        best = maxOf(best, run)
                    }
                    date == today -> Unit // pending
                    else -> run = 0
                }
            }
            date = date.plusDays(1)
        }
        return StreakInfo(current = current, best = maxOf(best, current), unit = StreakUnit.DAYS)
    }

    private fun weekStreak(
        habit: Habit,
        quota: Int,
        checkIns: List<HabitCheckIn>,
        today: LocalDate,
    ): StreakInfo {
        val completionsByWeek = checkIns.asSequence()
            .filter { it.habitId == habit.id && it.completed }
            .distinctBy { it.date }
            .groupBy { it.date.weekStart() }
            .mapValues { (_, completions) -> completions.size }
        val currentWeek = today.weekStart()
        val firstWeek = maxOf(habit.startDate, today.minusDays(MAX_LOOKBACK_DAYS)).weekStart()

        var current = 0
        var week = currentWeek
        while (!week.isBefore(firstWeek)) {
            val met = (completionsByWeek[week] ?: 0) >= quota
            when {
                met -> current++
                week == currentWeek -> Unit // in progress, not a break
                else -> break
            }
            week = week.minusWeeks(1)
        }

        var best = 0
        var run = 0
        week = firstWeek
        while (!week.isAfter(currentWeek)) {
            val met = (completionsByWeek[week] ?: 0) >= quota
            when {
                met -> {
                    run++
                    best = maxOf(best, run)
                }
                week == currentWeek -> Unit // pending
                else -> run = 0
            }
            week = week.plusWeeks(1)
        }
        return StreakInfo(current = current, best = maxOf(best, current), unit = StreakUnit.WEEKS)
    }

    private fun completedDates(habit: Habit, checkIns: List<HabitCheckIn>): Set<LocalDate> =
        checkIns.asSequence()
            .filter { it.habitId == habit.id && it.completed }
            .map { it.date }
            .toHashSet()

    companion object {
        /** Length of the rolling score window, in days. */
        const val SCORE_WINDOW_DAYS = 30L

        /** Score-point difference below which a change counts as STEADY. */
        const val TREND_THRESHOLD = 3

        /** Streak computation bound, so decade-old habits stay cheap. */
        const val MAX_LOOKBACK_DAYS = 730L
    }
}
