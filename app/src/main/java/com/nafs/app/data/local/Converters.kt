package com.nafs.app.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

/** LocalDate ↔ epoch-day. Epoch-day keeps SQL range queries (BETWEEN) correct. */
class Converters {

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}
