package com.justsoft.nulpschedule.utils

import android.content.Context
import android.text.format.DateFormat.getTimeFormat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.*
import javax.inject.Singleton

@Singleton
class TimeFormatter private constructor(
    private val context: Context,
    private val classesTimetable: ClassesTimetable
) {
    private val dateFormat: DateFormat
        get() = getTimeFormat(context)

    fun formatStartTimeForSubjectIndex(classIndex: Int): String =
        dateFormat.format(dateFromMinutes(classesTimetable.startTimes[classIndex - 1]))

    fun formatEndTimeForSubjectIndex(classIndex: Int): String =
        dateFormat.format(dateFromMinutes(classesTimetable.endTimes[classIndex - 1]))

    private fun dateFromMinutes(minutes: Int): Date {
        return Calendar
            .getInstance(TimeZone.getDefault())
            .apply {
                set(Calendar.HOUR_OF_DAY, minutes / 60)
                set(Calendar.MINUTE, minutes % 60)
            }.time

    }

    @Module
    @InstallIn(ApplicationComponent::class)
    class TimeFormatterModule {
        @Provides
        fun provideTimeFormatter(
            @ApplicationContext context: Context,
            classesTimetable: ClassesTimetable
        ): TimeFormatter = TimeFormatter(context, classesTimetable)

        @Provides
        fun provideTimetable(
            @ApplicationContext context: Context
        ): ClassesTimetable = ClassesTimetable(context)
    }
}