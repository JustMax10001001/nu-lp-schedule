package com.justsoft.nulpschedule.model

import androidx.room.Ignore

data class Subject(
    val id: Long,
    val scheduleId: Long,
    val subjectName: String,
    val customName: String? = null
) {
    @Ignore
    private var beautifiedSubjectName: String

    init {
        val partSuffix = subjectName.indexOf(", частина")
        beautifiedSubjectName = if (partSuffix > 0)
            subjectName.substring(0, partSuffix)
        else
            subjectName
        beautifiedSubjectName = beautifiedSubjectName.replace('`', '\'')
    }

    val displayName: String
        get() = customName ?: beautifiedSubjectName
}