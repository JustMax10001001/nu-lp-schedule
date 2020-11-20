package com.justsoft.nulpschedule.model

import com.justsoft.nulpschedule.api.model.ApiSchedule
import com.justsoft.nulpschedule.api.model.ApiScheduleClass
import com.justsoft.nulpschedule.api.model.ApiSubject
import com.justsoft.nulpschedule.db.model.*

fun ApiSchedule.toEntity() = EntitySchedule(
    id,
    instituteName,
    groupName,
    updateTime,
    wasNumeratorOnUpdate,
    addTime,
    scheduleType,
    subgroup,
    position
)

fun ApiSchedule.toUpdateEntity() = UpdateEntitySchedule(
    id, updateTime, wasNumeratorOnUpdate
)

fun ApiSubject.toEntity() = EntitySubject(id, scheduleId, subjectName)

fun ApiSubject.toUpdateEntity() = UpdateEntitySubject(id, subjectName)

fun ApiScheduleClass.toEntity() = EntityScheduleClass(
    id,
    subjectId,
    scheduleId,
    teacherName,
    classDescription,
    dayOfWeek,
    index,
    flags,
    url
)