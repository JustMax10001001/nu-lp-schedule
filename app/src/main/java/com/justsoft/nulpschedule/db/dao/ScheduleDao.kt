package com.justsoft.nulpschedule.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.justsoft.nulpschedule.api.model.ScheduleType
import com.justsoft.nulpschedule.db.model.EntitySchedule
import com.justsoft.nulpschedule.model.Schedule
import com.justsoft.nulpschedule.db.model.UpdateEntitySchedulePosition
import com.justsoft.nulpschedule.db.model.UpdateEntitySchedule
import com.justsoft.nulpschedule.model.InstituteAndGroup

@Dao
interface ScheduleDao {

    @Insert
    fun saveSchedule(schedule: EntitySchedule)

    @Update
    fun update(schedule: EntitySchedule)

    @Query("DELETE FROM Schedule WHERE id = :scheduleId")
    suspend fun deleteSchedule(scheduleId: Long)

    @Query("SELECT * FROM Schedule WHERE id = :scheduleId")
    fun getScheduleById(scheduleId: Long): Schedule

    @Query("SELECT * FROM Schedule WHERE id = :scheduleId")
    fun load(scheduleId: Long): LiveData<Schedule>

    @Query("SELECT * FROM SCHEDULE ORDER BY position, addTime ASC")
    fun loadAll(): LiveData<List<Schedule>>

    @Query("SELECT * FROM SCHEDULE")
    fun loadAllSync(): List<Schedule>

    @Update(entity = EntitySchedule::class)
    suspend fun updatePartial(schedule: UpdateEntitySchedule)

    @Update(entity = EntitySchedule::class)
    suspend fun updateSchedulePositions(schedulePositions: List<UpdateEntitySchedulePosition>)

    @Query("UPDATE Schedule SET subgroup = :newSubgroup WHERE id = :id")
    suspend fun updateSubgroup(id: Long, newSubgroup: Int)

    @Query("SELECT instituteName AS institute, groupName AS `group` FROM Schedule WHERE scheduleType = :scheduleType")
    suspend fun getPersistedInstitutesAndGroups(scheduleType: ScheduleType): List<InstituteAndGroup>

    @Query("DELETE FROM Schedule WHERE id IN (:scheduleIdsToDelete)")
    fun removeSchedules(scheduleIdsToDelete: Set<Long>)

}