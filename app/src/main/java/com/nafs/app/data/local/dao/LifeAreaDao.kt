package com.nafs.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nafs.app.data.local.entity.LifeAreaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeAreaDao {

    @Query("SELECT * FROM life_areas ORDER BY position, id")
    fun observeAll(): Flow<List<LifeAreaEntity>>

    @Query("SELECT * FROM life_areas ORDER BY position, id")
    suspend fun getAll(): List<LifeAreaEntity>

    @Query("SELECT * FROM life_areas WHERE id = :id")
    fun observeById(id: Long): Flow<LifeAreaEntity?>

    /** Returns the new row id on insert, -1 on update. */
    @Upsert
    suspend fun upsert(area: LifeAreaEntity): Long

    @Upsert
    suspend fun upsertAll(areas: List<LifeAreaEntity>)

    @Query("DELETE FROM life_areas WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM life_areas")
    suspend fun count(): Int
}
