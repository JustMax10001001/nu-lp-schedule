package com.justsoft.nulpschedule.db.dao

import androidx.room.*
import com.justsoft.nulpschedule.db.model.EntitySubject
import com.justsoft.nulpschedule.db.model.UpdateEntitySubject

@Dao
interface SubjectDao {

    @Insert
    fun save(subject: EntitySubject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAll(subjects: Collection<EntitySubject>)

    @Update
    suspend fun update(subject: EntitySubject)

    @Delete
    fun deleteAll(subjects: Collection<EntitySubject>)

    @Delete
    fun delete(subject: EntitySubject)

    @Query("DELETE FROM Subject WHERE scheduleId = :scheduleId AND id NOT IN (:idList)")
    fun deleteAllNotFromList(scheduleId: Long, idList: List<Long>)

    @Update(entity = EntitySubject::class)
    fun updateSubjects(subjects: List<UpdateEntitySubject>)

    @Query("UPDATE Subject SET customName = :newName WHERE id = :id")
    suspend fun updateSubjectName(id: Long, newName: String?)

    @Insert(entity = EntitySubject::class, onConflict = OnConflictStrategy.IGNORE)
    fun insertNew(subjectEntities: Collection<EntitySubject>)
}