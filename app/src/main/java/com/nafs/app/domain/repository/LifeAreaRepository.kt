package com.nafs.app.domain.repository

import com.nafs.app.domain.model.LifeArea
import kotlinx.coroutines.flow.Flow

interface LifeAreaRepository {

    /** All areas ordered by position. */
    fun observeAreas(): Flow<List<LifeArea>>

    fun observeArea(id: Long): Flow<LifeArea?>

    /** Inserts (id == 0) or updates; returns the persisted id. */
    suspend fun upsert(area: LifeArea): Long

    /** Deletes the area; its habits and their check-ins cascade. */
    suspend fun delete(id: Long)
}
