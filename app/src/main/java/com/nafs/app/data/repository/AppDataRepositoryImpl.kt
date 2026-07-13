package com.nafs.app.data.repository

import com.nafs.app.data.local.NafsDatabase
import com.nafs.app.data.seed.DatabaseSeeder
import com.nafs.app.di.IoDispatcher
import com.nafs.app.domain.repository.AppDataRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class AppDataRepositoryImpl @Inject constructor(
    private val database: NafsDatabase,
    private val seeder: DatabaseSeeder,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AppDataRepository {

    override suspend fun resetToDemoData() = withContext(io) {
        database.clearAllTables()
        seeder.seedDemoData()
    }

    override suspend fun clearAllData() = withContext(io) {
        database.clearAllTables()
        seeder.seedAreasOnly()
    }
}
