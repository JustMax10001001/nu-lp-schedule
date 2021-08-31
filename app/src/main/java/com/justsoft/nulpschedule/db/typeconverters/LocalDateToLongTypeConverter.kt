package com.justsoft.nulpschedule.db.typeconverters

import androidx.room.TypeConverter
import java.time.*

object LocalDateToLongTypeConverter {

    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.let {it.toEpochDay() * 24 * 3600 * 1000 }
    }
}

object LocalDateTimeToLongTypeConverter {

    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.let {it.toEpochSecond(ZoneOffset.systemDefault().rules.getOffset(it)) * 1000 }
    }
}