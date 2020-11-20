package com.justsoft.nulpschedule.db.model

import androidx.room.*

@Entity(
    tableName = "Subject",
    foreignKeys = [ForeignKey(
        entity = EntitySchedule::class,
        parentColumns = ["id"],
        childColumns = ["scheduleId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class EntitySubject(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(index = true)
    val scheduleId: Long,
    val subjectName: String,
    val customName: String? = null
)

@Entity
data class UpdateEntitySubject(
    @PrimaryKey
    val id: Long,
    val subjectName: String
)