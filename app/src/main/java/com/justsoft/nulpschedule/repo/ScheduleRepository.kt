package com.justsoft.nulpschedule.repo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.justsoft.nulpschedule.api.ScheduleApi
import com.justsoft.nulpschedule.api.model.ApiScheduleClass
import com.justsoft.nulpschedule.api.model.ApiSubject
import com.justsoft.nulpschedule.api.model.ScheduleType
import com.justsoft.nulpschedule.db.dao.ScheduleClassDao
import com.justsoft.nulpschedule.db.dao.ScheduleDao
import com.justsoft.nulpschedule.db.dao.SubjectDao
import com.justsoft.nulpschedule.db.model.*
import com.justsoft.nulpschedule.model.*
import com.justsoft.nulpschedule.utils.MutableStatefulLiveData
import com.justsoft.nulpschedule.utils.StatefulData.*
import com.justsoft.nulpschedule.utils.StatefulLiveData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao,
    private val classDao: ScheduleClassDao,
    private val subjectDao: SubjectDao,
    private val scheduleApi: ScheduleApi
) {
    private val instituteAndGroupListLiveData: MutableStatefulLiveData<List<InstituteAndGroup>> =
        MutableLiveData(
            Uninitialized()
        )

    suspend fun getInstitutesAndGroups(): StatefulLiveData<List<InstituteAndGroup>> = liveData {
        emitSource(instituteAndGroupListLiveData)

        if (instituteAndGroupListLiveData.value is Success)
            return@liveData

        withContext(Dispatchers.IO) {
            try {
                instituteAndGroupListLiveData.postValue(Loading())
                val alreadyDownloaded = getDownloadedInstitutesAndGroups()
                val jobs = mutableListOf<Job>()
                val institutesWithGroup = mutableListOf<InstituteAndGroup>()
                for (institute in getInstitutes().getOrThrow().sorted()) {
                    if (institute == "All")
                        continue
                    jobs.add(launch {
                        val groups = getGroups(institute).getOrThrow()
                        val result = groups.filter { it != "All" }
                            .map { InstituteAndGroup(institute, it) }
                            .filter { !alreadyDownloaded.contains(it) }
                        synchronized(institutesWithGroup) {
                            institutesWithGroup.addAll(result)
                        }
                    })
                }
                jobs.joinAll()
                instituteAndGroupListLiveData.postValue(Success(institutesWithGroup))
            } catch (e: Exception) {
                instituteAndGroupListLiveData.postValue(Error(e))
            }
        }
    }

    private suspend fun getDownloadedInstitutesAndGroups(scheduleType: ScheduleType = ScheduleType.STUDENT): List<InstituteAndGroup> =
        scheduleDao.getPersistedInstitutesAndGroups(scheduleType)

    private fun getInstitutes(): Result<List<String>> = Result.success(emptyList())

    private fun getGroups(instituteName: String): Result<List<String>> = Result.success(emptyList())

    fun refreshAllSchedules() {
        val schedules = scheduleDao.loadAllSync()
        for ((_, _, groupName) in schedules) {
            refreshScheduleSync(groupName)
        }
    }

    private fun refreshScheduleSync(groupName: String) {
        val boxedResult = scheduleApi.getSchedule(groupName)
        val (schedule, subjects, classes) = boxedResult.getOrThrow()
        scheduleDao.updatePartial(schedule.toUpdateEntity())

        subjectDao.insertNew(subjects.map { it.toEntity() })
        subjectDao.updateSubjects(subjects.map { it.toUpdateEntity() })

        val classEntities = classes.map { it.toEntity() }
        classDao.deleteAllNotFromList(schedule.id, classes.map { it.id })
        classDao.insertNew(classEntities)
        classDao.updateClasses(classEntities)
    }

    /**
     * Downloads the schedule and saves it locally
     * @return - the id of the downloaded schedule
     */
    suspend fun downloadSchedule(
        groupName: String,
        subgroup: Int
    ): Result<Long> =
        withContext(Dispatchers.IO) {
            val boxedResult = scheduleApi.getSchedule(groupName)
            if (boxedResult.isSuccess) {
                val unboxedResult = boxedResult.getOrNull()!!
                scheduleDao.saveSchedule(unboxedResult.schedule.toEntity())
                scheduleDao.update(unboxedResult.schedule.copy(subgroup = subgroup).toEntity())
                subjectDao.saveAll(unboxedResult.subjects.map(ApiSubject::toEntity))
                classDao.saveAll(unboxedResult.classes.map(ApiScheduleClass::toEntity))
                return@withContext Result.success(unboxedResult.schedule.id)
            }
            return@withContext Result.failure(boxedResult.exceptionOrNull()!!)
        }

    fun getSchedule(scheduleId: Long): LiveData<Schedule> {
        return scheduleDao.load(scheduleId)
    }

    fun getClassesWithSubjects(
        scheduleId: Long,
        dayOfWeek: DayOfWeek
    ): LiveData<List<EntityClassWithSubject>> =
        classDao.getClassesAndSubjectsForScheduleAndDay(scheduleId, dayOfWeek)

    fun getSchedules(): LiveData<List<Schedule>> {
        return scheduleDao.loadAll()
    }

    fun getSubjectCountForSchedule(scheduleId: Long): LiveData<Int> =
        classDao.countActiveSubjectsForSchedule(scheduleId)

    suspend fun getClassesWithSubjectsSync(
        scheduleId: Long,
        dayOfWeek: DayOfWeek,
        startIndex: Int = 0
    ): List<EntityClassWithSubject> =
        classDao.getClassesAndSubjectsForScheduleAndDaySync(scheduleId, dayOfWeek, startIndex)

    suspend fun getClassWithSubjects(
        scheduleId: Long,
        dayOfWeek: DayOfWeek,
        subjectIndex: Int
    ): List<EntityClassWithSubject> {
        return classDao.getClassWithSubjectForDayAtIndex(scheduleId, dayOfWeek, subjectIndex)
    }

    suspend fun updateSchedulePositions(schedulePositions: List<UpdateEntitySchedulePosition>) {
        scheduleDao.updateSchedulePositions(schedulePositions)
    }

    suspend fun updateSubjectName(id: Long, newName: String?) {
        subjectDao.updateSubjectName(id, newName)
    }

    suspend fun updateSubgroup(id: Long, newSubgroup: Int) {
        scheduleDao.updateSubgroup(id, newSubgroup)
    }

    fun removeSchedules(scheduleIdsToDelete: Set<Long>) {
        scheduleDao.removeSchedules(scheduleIdsToDelete)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ScheduleRepositoryModule {

    @Provides
    @Singleton
    fun scheduleRepository(
        subjectDao: SubjectDao,
        scheduleDao: ScheduleDao,
        scheduleDayDao: ScheduleClassDao,
        scheduleApi: ScheduleApi
    ): ScheduleRepository =
        ScheduleRepository(scheduleDao, scheduleDayDao, subjectDao, scheduleApi)
}