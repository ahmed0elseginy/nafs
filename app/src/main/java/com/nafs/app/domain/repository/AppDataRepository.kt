package com.nafs.app.domain.repository

/** Bulk, destructive operations over the whole database, exposed to Settings. */
interface AppDataRepository {

    /** Wipes everything and reloads the demo areas, habits, and history. */
    suspend fun resetToDemoData()

    /** Wipes everything and re-creates only the empty preset life areas. */
    suspend fun clearAllData()
}
