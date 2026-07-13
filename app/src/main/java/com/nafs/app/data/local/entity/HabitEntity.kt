package com.nafs.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Frequency is flattened into three columns: [frequencyType] discriminates,
 * [timesPerWeek] applies to WEEKLY, [daysMask] (bit per `DayOfWeek.value`)
 * applies to SPECIFIC_DAYS.
 */
@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = LifeAreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["area_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("area_id")],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "area_id") val areaId: Long,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "frequency_type") val frequencyType: String,
    @ColumnInfo(name = "times_per_week") val timesPerWeek: Int,
    @ColumnInfo(name = "days_mask") val daysMask: Int,
    @ColumnInfo(name = "start_date") val startDate: LocalDate,
    @ColumnInfo(name = "duration_days") val durationDays: Int?,
    @ColumnInfo(name = "target_score") val targetScore: Int,
    @ColumnInfo(name = "reminder_minute") val reminderMinute: Int?,
)
