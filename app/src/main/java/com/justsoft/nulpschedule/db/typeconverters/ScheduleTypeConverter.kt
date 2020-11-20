package com.justsoft.nulpschedule.db.typeconverters

import androidx.room.TypeConverter
import com.justsoft.nulpschedule.api.model.ScheduleType

object ScheduleTypeConverter {

    @TypeConverter
    fun fromOrdinal(ordinal: Int) = ScheduleType.values()[ordinal]

    @TypeConverter
    fun toOrdinal(type: ScheduleType): Int = type.ordinal
}