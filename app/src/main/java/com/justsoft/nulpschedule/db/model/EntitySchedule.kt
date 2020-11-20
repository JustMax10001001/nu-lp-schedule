package com.justsoft.nulpschedule.db.model

import androidx.room.*
import com.justsoft.nulpschedule.api.model.ScheduleType
import com.justsoft.nulpschedule.model.Schedule
import java.time.LocalDateTime

@Entity(
    tableName = "Schedule"
)
data class EntitySchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val instituteName: String,
    val groupName: String,
    val updateTime: LocalDateTime,
    val wasNumeratorOnUpdate: Boolean,
    val addTime: LocalDateTime,
    @ColumnInfo(defaultValue = "0")
    val scheduleType: ScheduleType,
    @ColumnInfo(defaultValue = "1")
    val subgroup: Int = 1,
    @ColumnInfo(defaultValue = "NULL")
    val position: Int? = null
)

@Entity
data class UpdateEntitySchedulePosition(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(defaultValue = "NULL")
    val position: Int?
)

@Entity
data class UpdateEntitySchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val updateTime: LocalDateTime,
    val wasNumeratorOnUpdate: Boolean
)

data class ScheduleTuple(
    val schedule: Schedule,
    val currentClass: EntityClassWithSubject?,
    val nextClass: EntityClassWithSubject?
) {
    val scheduleId = schedule.id
}