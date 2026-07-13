package com.nafs.app.data.mapper

import com.nafs.app.data.local.entity.HabitCheckInEntity
import com.nafs.app.data.local.entity.HabitEntity
import com.nafs.app.data.local.entity.LifeAreaEntity
import com.nafs.app.domain.model.Habit
import com.nafs.app.domain.model.HabitCheckIn
import com.nafs.app.domain.model.HabitFrequency
import com.nafs.app.domain.model.LifeArea
import java.time.DayOfWeek
import java.time.LocalTime

/** Discriminator values stored in `habits.frequency_type`. */
enum class FrequencyType { DAILY, WEEKLY, SPECIFIC_DAYS }

fun LifeAreaEntity.toDomain() = LifeArea(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    weight = weight,
    position = position,
)

fun LifeArea.toEntity() = LifeAreaEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    weight = weight,
    position = position,
)

fun HabitEntity.toDomain() = Habit(
    id = id,
    areaId = areaId,
    name = name,
    description = description,
    frequency = when (FrequencyType.valueOf(frequencyType)) {
        FrequencyType.DAILY -> HabitFrequency.Daily
        FrequencyType.WEEKLY -> HabitFrequency.Weekly(timesPerWeek)
        FrequencyType.SPECIFIC_DAYS -> HabitFrequency.SpecificDays(decodeDaysMask(daysMask))
    },
    startDate = startDate,
    durationDays = durationDays,
    targetScore = targetScore,
    reminderTime = reminderMinute?.let { LocalTime.of(it / 60, it % 60) },
)

fun Habit.toEntity() = HabitEntity(
    id = id,
    areaId = areaId,
    name = name,
    description = description,
    frequencyType = when (frequency) {
        HabitFrequency.Daily -> FrequencyType.DAILY
        is HabitFrequency.Weekly -> FrequencyType.WEEKLY
        is HabitFrequency.SpecificDays -> FrequencyType.SPECIFIC_DAYS
    }.name,
    timesPerWeek = (frequency as? HabitFrequency.Weekly)?.timesPerWeek ?: 0,
    daysMask = (frequency as? HabitFrequency.SpecificDays)?.days?.let(::encodeDaysMask) ?: 0,
    startDate = startDate,
    durationDays = durationDays,
    targetScore = targetScore,
    reminderMinute = reminderTime?.let { it.hour * 60 + it.minute },
)

fun HabitCheckInEntity.toDomain() = HabitCheckIn(
    id = id,
    habitId = habitId,
    date = date,
    completed = completed,
    note = note,
)

fun HabitCheckIn.toEntity() = HabitCheckInEntity(
    id = id,
    habitId = habitId,
    date = date,
    completed = completed,
    note = note,
)

/** One bit per weekday, `1 shl (DayOfWeek.value - 1)` — Monday is bit 0, Sunday bit 6. */
fun encodeDaysMask(days: Set<DayOfWeek>): Int =
    days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

fun decodeDaysMask(mask: Int): Set<DayOfWeek> =
    DayOfWeek.entries.filterTo(mutableSetOf()) { (mask shr (it.value - 1)) and 1 == 1 }
