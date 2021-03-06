package com.justsoft.nulpschedule.model

import com.justsoft.nulpschedule.api.model.ScheduleType
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

data class Schedule(
    val id: Long = 0,
    val instituteName: String,
    val groupName: String,
    val updateTime: LocalDateTime,
    val wasNumeratorOnUpdate: Boolean,
    val addTime: LocalDateTime,
    val scheduleType: ScheduleType,
    val subgroup: Int = 1,
    val position: Int? = null
) {

    fun isNumeratorOnDate(dateNow: LocalDateTime, dayToSwitchToNextWeekOn: Int = 5): Boolean {
        // the site updates numerator\denominator on Monday on 00:00
        // we want to show user the next weeks schedule as early as on Saturday

        // if we request update on Saturday or Sunday, perform this request on next virtual Monday
        // first we check if dayToSwitchToNextWeekOn is set to Sat or Sun
        // then we check current day against the variable
        val correctedDateNow =
            if (dayToSwitchToNextWeekOn >= 5 && dateNow.dayOfWeek.ordinal >= dayToSwitchToNextWeekOn)
                dateNow.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            else
                dateNow

        val mondayBeforeUpdateTime =
            updateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val mondayAfterNow = correctedDateNow.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
        val dayDelta = mondayAfterNow.minusDays(mondayBeforeUpdateTime.toLocalDate().toEpochDay())
            .toLocalDate().toEpochDay()
        return (dayDelta % 14 < 7.toLong()) == wasNumeratorOnUpdate
    }
}