package com.justsoft.nulpschedule.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.db.model.EntityScheduleClass
import java.time.DayOfWeek

@Dao
interface ScheduleClassDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAll(classes: List<EntityScheduleClass>)

    @Transaction
    @Query("SELECT * FROM ScheduleClass WHERE scheduleId = :scheduleId AND dayOfWeek = :dayOfWeek ORDER BY `index` ASC")
    fun getClassesAndSubjectsForScheduleAndDay(
        scheduleId: Long,
        dayOfWeek: DayOfWeek
    ): LiveData<List<EntityClassWithSubject>>

    @Transaction
    @Query("SELECT * FROM ScheduleClass WHERE scheduleId = :scheduleId AND dayOfWeek = :dayOfWeek AND `index` = :subjectIndex")
    suspend fun getClassWithSubjectForDayAtIndex(
        scheduleId: Long,
        dayOfWeek: DayOfWeek,
        subjectIndex: Int
    ): List<EntityClassWithSubject>

    @Transaction
    @Query("SELECT * FROM ScheduleClass WHERE scheduleId = :scheduleId AND dayOfWeek = :dayOfWeek AND `index` >= :startIndex ORDER BY `index` ASC")
    suspend fun getClassesAndSubjectsForScheduleAndDaySync(
        scheduleId: Long,
        dayOfWeek: DayOfWeek,
        startIndex: Int
    ): List<EntityClassWithSubject>

    @Query("DELETE FROM ScheduleClass WHERE scheduleId = :scheduleId AND id NOT IN (:idList)")
    fun deleteAllNotFromList(scheduleId: Long, idList: List<Long>)

    @Update
    fun updateClasses(classes: List<EntityScheduleClass>)

    @Insert(entity = EntityScheduleClass::class, onConflict = OnConflictStrategy.IGNORE)
    fun insertNew(classEntities: Collection<EntityScheduleClass>)

    @Query("SELECT COUNT(DISTINCT subjectId) FROM ScheduleClass WHERE scheduleId = :scheduleId")
    fun countActiveSubjectsForSchedule(scheduleId: Long): LiveData<Int>
}