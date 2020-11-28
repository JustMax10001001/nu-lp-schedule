package com.justsoft.nulpschedule.fragments.scheduleselectfragment

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.justsoft.nulpschedule.db.model.*
import com.justsoft.nulpschedule.model.RefreshState
import com.justsoft.nulpschedule.model.Schedule
import com.justsoft.nulpschedule.repo.ScheduleRepository
import com.justsoft.nulpschedule.utils.delegateLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.timerTask

class ScheduleSelectViewModel @ViewModelInject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    val scheduleListLiveData = scheduleRepository.getSchedules()

    val currentClassIndexLiveData = MutableLiveData(-1)
    val currentClassIndex by delegateLiveData(currentClassIndexLiveData)

    val nextClassIndexLiveData = MutableLiveData(-1)
    val nextClassIndex by delegateLiveData(nextClassIndexLiveData)

    val currentDayOfWeekLiveData: MutableLiveData<DayOfWeek> =
        MutableLiveData(LocalDate.now().dayOfWeek)
    val currentDayOfWeek by delegateLiveData(currentDayOfWeekLiveData)

    private val deleteScheduleTimer = Timer()
    private var scheduleDeletionTask: TimerTask? = null
    private val scheduleIdsToDelete = mutableSetOf<Long>()

    val scheduleTupleListLiveData: LiveData<List<ScheduleTuple>> =
        MediatorLiveData<List<ScheduleTuple>>().apply {
            value = emptyList()
            addSource(scheduleListLiveData) { schedules ->
                viewModelScope.launch(Dispatchers.IO) {
                    val scheduleTupleList = createScheduleTupleList(schedules)
                    postValue(scheduleTupleList)
                }
            }
            addSource(currentClassIndexLiveData) {
                scheduleListLiveData.value ?: return@addSource
                viewModelScope.launch(Dispatchers.IO) {
                    val scheduleTupleList =
                        createScheduleTupleList(scheduleListLiveData.value ?: return@launch)
                    postValue(scheduleTupleList)
                }
            }
            addSource(nextClassIndexLiveData) {
                scheduleListLiveData.value ?: return@addSource
                viewModelScope.launch(Dispatchers.IO) {
                    val scheduleTupleList =
                        createScheduleTupleList(scheduleListLiveData.value ?: return@launch)
                    postValue(scheduleTupleList)
                }
            }
        }

    private suspend fun createScheduleTupleList(
        schedules: List<Schedule>
    ): List<ScheduleTuple> {
        return schedules
            .filter { !scheduleIdsToDelete.contains(it.id) }
            .map { schedule ->
                val currentClass = getCurrentClassForSchedule(schedule)
                val nextClass = getNextClassForSchedule(schedule)
                ScheduleTuple(
                    schedule,
                    currentClass,
                    nextClass
                )
            }.sortedBy { it.schedule.position }
    }

    private suspend fun getNextClassForSchedule(schedule: Schedule): EntityClassWithSubject? {
        var dow = currentDayOfWeek
        val isNumerator = schedule.isNumeratorOnDate(LocalDateTime.now())
        do {
            val classList = if (dow == currentDayOfWeek)        // first iteration
                scheduleRepository.getClassesWithSubjectsSync(schedule.id, dow, nextClassIndex)
            else
                scheduleRepository.getClassesWithSubjectsSync(schedule.id, dow)
            val classToReturn = classList.firstOrNull {
                it.scheduleClass.classMatches(
                    schedule.subgroup,
                    isNumerator
                )
            }
            if (classToReturn != null) {
                //Log.d("SSVM", "Selected next class $classToReturn for schedule $schedule")
                return classToReturn
            }

            dow = dow.nextWorkDay()
        } while (dow != currentDayOfWeek)
        return null
    }

    private fun DayOfWeek.nextWorkDay(): DayOfWeek {
        if (this.ordinal >= 4)
            return DayOfWeek.MONDAY
        return DayOfWeek.values()[this.ordinal + 1]
    }

    private suspend fun getCurrentClassForSchedule(
        schedule: Schedule
    ): EntityClassWithSubject? {
        if (currentClassIndex == -1)
            return null

        return scheduleRepository.getClassWithSubjects(
            schedule.id,
            currentDayOfWeek,
            currentClassIndex
        ).firstOrNull {
            it.scheduleClass.classMatches(
                schedule.subgroup,
                schedule.isNumeratorOnDate(LocalDateTime.now())
            )
        }
    }

    fun postScheduleForDeletion(schedule: Schedule) {
        scheduleDeletionTask?.cancel()
        scheduleDeletionTask = timerTask {
            synchronized(scheduleIdsToDelete) {
                scheduleRepository.removeSchedules(scheduleIdsToDelete)
                scheduleIdsToDelete.clear()
                scheduleDeletionTask = null
            }
        }
        synchronized(scheduleIdsToDelete) {
            scheduleIdsToDelete.add(schedule.id)
        }
        deleteScheduleTimer.schedule(scheduleDeletionTask, 10 * 1000)
    }

    fun cancelDeletion() {
        scheduleDeletionTask?.cancel()
    }

    fun refreshSchedules(): StateFlow<RefreshState> {
        val flow = MutableStateFlow(RefreshState.PREPARING)
        viewModelScope.launch {
            try {
                scheduleRepository.refreshAllSchedules()
                flow.emit(RefreshState.REFRESH_SUCCESS)
            } catch (e: Exception) {
                Log.e("ScheduleSelectViewModel", "Error refreshing schedules", e)
                flow.emit(RefreshState.REFRESH_FAILED)
            }
        }
        return flow.asStateFlow()
    }

    fun updateSchedulePositions(schedulePositions: List<UpdateEntitySchedulePosition>) =
        viewModelScope.launch {
            scheduleRepository.updateSchedulePositions(schedulePositions)
        }
}