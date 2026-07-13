package com.nafs.app.data.seed

import android.content.Context
import com.nafs.app.R
import com.nafs.app.data.local.entity.LifeAreaEntity
import com.nafs.app.data.mapper.FrequencyType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A demo habit spec, tied to a seed area by its [areaPosition]. [completionBias]
 * is the baseline probability that a scheduled day was completed when generating
 * history; it drifts upward over time so the demo shows an improving trend.
 */
data class DemoHabit(
    val areaPosition: Int,
    val name: String,
    val frequencyType: FrequencyType,
    val timesPerWeek: Int = 0,
    val days: Set<DayOfWeek> = emptySet(),
    val targetScore: Int = 100,
    val completionBias: Double,
)

/**
 * The six preset life areas inserted on first launch, plus a set of demo habits
 * and (via [DatabaseSeeder]) several weeks of history so the charts and the
 * Overall tab are populated from the very first launch. Names resolve against the
 * locale active at that moment; they are plain, fully editable user data afterwards.
 */
@Singleton
class SeedDataProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun defaultAreas(): List<LifeAreaEntity> = listOf(
        area(R.string.seed_area_quran, "menu_book", 0xFF2E7D57, weight = 1.5f, position = 0),
        area(R.string.seed_area_study, "school", 0xFF3949AB, weight = 1f, position = 1),
        area(R.string.seed_area_health, "monitor_heart", 0xFFAD1457, weight = 1f, position = 2),
        area(R.string.seed_area_fitness, "fitness_center", 0xFFB3542E, weight = 1f, position = 3),
        area(R.string.seed_area_english, "translate", 0xFF00838F, weight = 1f, position = 4),
        area(R.string.seed_area_work, "work", 0xFF546E7A, weight = 1f, position = 5),
    )

    fun demoHabits(): List<DemoHabit> = listOf(
        DemoHabit(
            areaPosition = 0,
            name = context.getString(R.string.seed_habit_read_quran),
            frequencyType = FrequencyType.DAILY,
            completionBias = 0.72,
        ),
        DemoHabit(
            areaPosition = 0,
            name = context.getString(R.string.seed_habit_memorize),
            frequencyType = FrequencyType.WEEKLY,
            timesPerWeek = 3,
            completionBias = 0.6,
        ),
        DemoHabit(
            areaPosition = 1,
            name = context.getString(R.string.seed_habit_study),
            frequencyType = FrequencyType.SPECIFIC_DAYS,
            days = setOf(DayOfWeek.SATURDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            completionBias = 0.58,
        ),
        DemoHabit(
            areaPosition = 2,
            name = context.getString(R.string.seed_habit_sleep),
            frequencyType = FrequencyType.DAILY,
            completionBias = 0.5,
        ),
        DemoHabit(
            areaPosition = 2,
            name = context.getString(R.string.seed_habit_water),
            frequencyType = FrequencyType.DAILY,
            completionBias = 0.66,
        ),
        DemoHabit(
            areaPosition = 3,
            name = context.getString(R.string.seed_habit_workout),
            frequencyType = FrequencyType.SPECIFIC_DAYS,
            days = setOf(DayOfWeek.SATURDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            completionBias = 0.55,
        ),
        DemoHabit(
            areaPosition = 4,
            name = context.getString(R.string.seed_habit_english),
            frequencyType = FrequencyType.WEEKLY,
            timesPerWeek = 4,
            completionBias = 0.62,
        ),
        DemoHabit(
            areaPosition = 5,
            name = context.getString(R.string.seed_habit_deep_work),
            frequencyType = FrequencyType.SPECIFIC_DAYS,
            days = setOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
            ),
            completionBias = 0.68,
        ),
    )

    private fun area(nameRes: Int, iconKey: String, colorArgb: Long, weight: Float, position: Int) =
        LifeAreaEntity(
            name = context.getString(nameRes),
            iconKey = iconKey,
            colorArgb = colorArgb,
            weight = weight,
            position = position,
        )
}
