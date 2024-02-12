package com.justsoft.nulpschedule.db.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.justsoft.nulpschedule.model.ScheduleClass
import com.justsoft.nulpschedule.model.Subject
import java.time.DayOfWeek

@Entity(
    tableName = "ScheduleClass",
    foreignKeys = [ForeignKey(
        entity = EntitySchedule::class,
        parentColumns = ["id"],
        childColumns = ["scheduleId"],
        onDelete = CASCADE
    ), ForeignKey(
        entity = EntitySubject::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = CASCADE
    )]
)
data class EntityScheduleClass(

    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(index = true)
    val subjectId: Long,
    @ColumnInfo(index = true)
    val scheduleId: Long,
    val teacherName: String,
    val classDescription: String,
    val dayOfWeek: DayOfWeek,
    val index: Int,
    val flags: Int,
    val url: String?
) {
    companion object {

        const val FLAG_SUBGROUP_1 = 1 shl 0
        const val FLAG_SUBGROUP_2 = 1 shl 1

        const val FLAG_NUMERATOR = 1 shl 2
        const val FLAG_DENOMINATOR = 1 shl 3
    }
}

data class EntityClassWithSubject(
    @Embedded
    val scheduleClass: ScheduleClass,
    @Relation(
        parentColumn = "subjectId",
        entityColumn = "id",
        entity = EntitySubject::class
    )
    val subject: Subject
)
