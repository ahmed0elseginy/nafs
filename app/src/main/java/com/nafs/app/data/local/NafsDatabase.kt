package com.nafs.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nafs.app.data.local.dao.CheckInDao
import com.nafs.app.data.local.dao.HabitDao
import com.nafs.app.data.local.dao.LifeAreaDao
import com.nafs.app.data.local.entity.HabitCheckInEntity
import com.nafs.app.data.local.entity.HabitEntity
import com.nafs.app.data.local.entity.LifeAreaEntity

/**
 * Phase 1 schema: life areas, habits, check-ins. Plans, milestones and status
 * snapshots arrive as migrations in later phases.
 */
@Database(
    entities = [
        LifeAreaEntity::class,
        HabitEntity::class,
        HabitCheckInEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NafsDatabase : RoomDatabase() {
    abstract fun lifeAreaDao(): LifeAreaDao
    abstract fun habitDao(): HabitDao
    abstract fun checkInDao(): CheckInDao
}
