package com.nafs.app.di

import android.content.Context
import androidx.room.Room
import com.nafs.app.data.local.NafsDatabase
import com.nafs.app.data.local.dao.CheckInDao
import com.nafs.app.data.local.dao.HabitDao
import com.nafs.app.data.local.dao.LifeAreaDao
import com.nafs.app.data.seed.SeedDatabaseCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        seedCallback: SeedDatabaseCallback,
    ): NafsDatabase =
        Room.databaseBuilder(context, NafsDatabase::class.java, "nafs.db")
            .addCallback(seedCallback)
            .build()

    @Provides
    fun provideLifeAreaDao(database: NafsDatabase): LifeAreaDao = database.lifeAreaDao()

    @Provides
    fun provideHabitDao(database: NafsDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideCheckInDao(database: NafsDatabase): CheckInDao = database.checkInDao()
}
