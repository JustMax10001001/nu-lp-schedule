package com.justsoft.nulpschedule.utils

import android.content.Context
import com.justsoft.nulpschedule.R
import javax.inject.Singleton

@Singleton
class ClassesTimetable constructor(context: Context) {
    val startTimes = context.resources.getIntArray(R.array.schedule_lesson_start_times)
    val endTimes = context.resources.getIntArray(R.array.schedule_lesson_end_times)

    fun findNextClassIndex(numMinutes: Int): Int {
        for (i in startTimes.indices) {
            if (startTimes[i] > numMinutes)
                return i + 1
        }
        return Int.MAX_VALUE        // to prevent the front page from showing first class as next after the last class
    }

    fun findCurrentClassIndex(numMinutes: Int): Int {
        for (i in endTimes.indices) {
            if (startTimes[i] <= numMinutes && numMinutes <= endTimes[i])
                return i + 1
        }
        return Int.MIN_VALUE
    }
}