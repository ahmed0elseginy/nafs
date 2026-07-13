package com.nafs.app.data.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nafs.app.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Seeds the preset areas, demo habits, and their history exactly once, when Room
 * first creates the database file.
 */
@Singleton
class SeedDatabaseCallback @Inject constructor(
    private val seeder: DatabaseSeeder,
    @ApplicationScope private val scope: CoroutineScope,
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch { seeder.seedDemoData() }
    }
}
