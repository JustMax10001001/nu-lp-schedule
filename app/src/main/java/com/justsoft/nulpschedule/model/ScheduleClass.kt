package com.justsoft.nulpschedule.model

import com.justsoft.nulpschedule.db.model.EntityScheduleClass.Companion.FLAG_DENOMINATOR
import com.justsoft.nulpschedule.db.model.EntityScheduleClass.Companion.FLAG_NUMERATOR
import com.justsoft.nulpschedule.db.model.EntityScheduleClass.Companion.FLAG_SUBGROUP_1
import com.justsoft.nulpschedule.db.model.EntityScheduleClass.Companion.FLAG_SUBGROUP_2
import java.time.DayOfWeek

@Suppress("MemberVisibilityCanBePrivate")
data class ScheduleClass(
    val id: Long,
    val subjectId: Long,
    val scheduleId: Long,
    val teacherName: String,
    val classDescription: String,
    val dayOfWeek: DayOfWeek,
    val index: Int,
    val flags: Int,
    val url: String?
) {

    val isOnline: Boolean
        get() = url != null

    val isForFirstSubgroup: Boolean
        get() = flags chkFlag FLAG_SUBGROUP_1

    val isForSecondSubgroup: Boolean
        get() = flags chkFlag FLAG_SUBGROUP_2

    val isSubgroupAgnostic: Boolean
        get() = isForFirstSubgroup && isForSecondSubgroup

    val isNumerator: Boolean
        get() = flags chkFlag FLAG_NUMERATOR

    val isDenominator: Boolean
        get() = flags chkFlag FLAG_DENOMINATOR

    val isWeekAgnostic: Boolean
        get() = isNumerator && isDenominator

    fun classMatches(subgroup: Int, isEnumerator: Boolean) =
        (isForFirstSubgroup == (subgroup == 1) || isSubgroupAgnostic) &&
                (isNumerator == isEnumerator || isWeekAgnostic)

    private infix fun Int.chkFlag(flag: Int): Boolean = this and flag > 0
}