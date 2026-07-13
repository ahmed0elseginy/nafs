package com.nafs.app.di

import com.nafs.app.data.repository.AppDataRepositoryImpl
import com.nafs.app.data.repository.CheckInRepositoryImpl
import com.nafs.app.data.repository.HabitRepositoryImpl
import com.nafs.app.data.repository.LifeAreaRepositoryImpl
import com.nafs.app.data.repository.SettingsRepositoryImpl
import com.nafs.app.domain.repository.AppDataRepository
import com.nafs.app.domain.repository.CheckInRepository
import com.nafs.app.domain.repository.HabitRepository
import com.nafs.app.domain.repository.LifeAreaRepository
import com.nafs.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindLifeAreaRepository(impl: LifeAreaRepositoryImpl): LifeAreaRepository

    @Binds
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    @Binds
    abstract fun bindCheckInRepository(impl: CheckInRepositoryImpl): CheckInRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindAppDataRepository(impl: AppDataRepositoryImpl): AppDataRepository
}
