package com.nafs.app.data.repository

import com.nafs.app.data.local.dao.LifeAreaDao
import com.nafs.app.data.mapper.toDomain
import com.nafs.app.data.mapper.toEntity
import com.nafs.app.di.IoDispatcher
import com.nafs.app.domain.model.LifeArea
import com.nafs.app.domain.repository.LifeAreaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class LifeAreaRepositoryImpl @Inject constructor(
    private val dao: LifeAreaDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LifeAreaRepository {

    override fun observeAreas(): Flow<List<LifeArea>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeArea(id: Long): Flow<LifeArea?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun upsert(area: LifeArea): Long = withContext(io) {
        // New areas go to the end of the list.
        val toSave = if (area.id == 0L) area.copy(position = dao.count()) else area
        val rowId = dao.upsert(toSave.toEntity())
        if (toSave.id != 0L) toSave.id else rowId
    }

    override suspend fun delete(id: Long) = withContext(io) {
        dao.deleteById(id)
    }
}
