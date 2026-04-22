package com.pocketsave.data.local.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room type converters. Dates are stored as epoch millis so the schema survives
 * timezone and locale changes.
 */
class Converters {
    @TypeConverter
    fun dateToLong(date: Date?): Long? = date?.time

    @TypeConverter
    fun longToDate(value: Long?): Date? = value?.let(::Date)
}
