package com.justsoft.nulpschedule.db.typeconverters

import androidx.room.TypeConverter
import java.time.DayOfWeek

object DayOfWeekTypeConverter {
    @TypeConverter
    fun fromEnumToInt(dayOfWeek: DayOfWeek) = dayOfWeek.ordinal

    @TypeConverter
    fun fromIntToEnum(ordinal: Int) = enumValues<DayOfWeek>()[ordinal]
}