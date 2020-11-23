package com.justsoft.nulpschedule.repo

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Exception

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

    @Suppress("DeferredResultUnused")
    suspend fun getInstitutesAndGroups(): StatefulLiveData<List<InstituteAndGroup>> {
        if (instituteAndGroupListLiveData.value !is Success) {
            withContext(Dispatchers.IO) {
                async {
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
        }
        return instituteAndGroupListLiveData
    }

    private suspend fun getDownloadedInstitutesAndGroups(scheduleType: ScheduleType = ScheduleType.STUDENT): List<InstituteAndGroup> =
        scheduleDao.getPersistedInstitutesAndGroups(scheduleType)

    private fun getInstitutes(): Result<List<String>> =
        scheduleApi.getInstitutes()

    private fun getGroups(instituteName: String): Result<List<String>> =
        scheduleApi.getGroups(instituteName)

    private suspend fun refreshSchedule(instituteName: String, groupName: String) {
        withContext(Dispatchers.IO) {
            refreshScheduleSync(instituteName, groupName)
        }
    }

    suspend fun refreshSchedule(scheduleId: Long): StateFlow<RefreshState> {
        val stateFlow = MutableStateFlow(RefreshState.PREPARING)
        withContext(Dispatchers.IO) {
            try {
                val schedule = scheduleDao.getScheduleById(scheduleId)
                refreshSchedule(schedule.instituteName, schedule.groupName)
                stateFlow.emit(RefreshState.REFRESH_SUCCESS)
            } catch (e: Exception) {
                Log.e("ScheduleRepository", "Error while refreshing schedule", e)
                stateFlow.emit(RefreshState.REFRESH_FAILED)
            }
        }
        return stateFlow.asStateFlow()
    }

    fun refreshAllSchedulesSync() {
        val schedules = scheduleDao.loadAllSync()
        for ((_, instituteName, groupName) in schedules) {
            refreshScheduleSync(instituteName, groupName)
        }
    }

    private fun refreshScheduleSync(instituteName: String, groupName: String) {
        val boxedResult = scheduleApi.getSchedule(instituteName, groupName)
        val (schedule, subjects, classes) = boxedResult.getOrThrow()
        scheduleDao.updatePartial(schedule.toUpdateEntity())

        subjectDao.deleteAllNotFromList(schedule.id, subjects.map { it.id })
        subjectDao.updateSubjects(subjects.map { it.toUpdateEntity() })

        classDao.deleteAllNotFromList(schedule.id, classes.map { it.id })
        classDao.updateClasses(classes.map { it.toEntity() })
    }

    suspend fun refreshAllSchedules(): StateFlow<RefreshState> = withContext(Dispatchers.IO) {
        val stateFlow = MutableStateFlow(RefreshState.PREPARING)
        @Suppress("DeferredResultUnused")
        async {
            try {
                refreshAllSchedulesSync()
                Log.d("ScheduleRepository", "Successfully refreshed schedules")
                stateFlow.emit(RefreshState.REFRESH_SUCCESS)
            } catch (e: Exception) {
                Log.e("ScheduleRepository", "Refresh schedules failed", e)
                stateFlow.emit(RefreshState.REFRESH_FAILED)
            }
        }
        Log.d("ScheduleRepository", "Returning")
        return@withContext stateFlow.asStateFlow()
    }

    /**
     * Downloads the schedule and saves it locally
     * @return - the id of the downloaded schedule
     */
    suspend fun downloadSchedule(
        instituteName: String,
        groupName: String,
        subgroup: Int
    ): Result<Long> =
            withContext(Dispatchers.IO) {
                val boxedResult = scheduleApi.getSchedule(instituteName, groupName)
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

    suspend fun removeSchedule(schedule: Schedule) {
        scheduleDao.deleteSchedule(schedule.id)
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

enum class RefreshState {
    PREPARING, REFRESH_FAILED, REFRESH_SUCCESS
}

@Module
@InstallIn(ApplicationComponent::class)
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