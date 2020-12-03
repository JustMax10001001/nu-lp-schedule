package com.justsoft.nulpschedule.fragments.dayviewfragment

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.model.Schedule
import com.justsoft.nulpschedule.repo.ScheduleRepository
import com.justsoft.nulpschedule.utils.isInitialized
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime

class DayViewFragmentViewModel @ViewModelInject constructor(
    private val scheduleRepository: ScheduleRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val dayOfWeekId: Int
        get() = savedStateHandle.get("schedule_day")!!

    val scheduleId: Long
        get() = savedStateHandle.get("schedule_id")!!

    private val dayToSwitchToNextWeekOn: Int
        get() = savedStateHandle.get("day_to_switch_to_next_week_on")!!

    val scheduleLiveData: LiveData<Schedule> by lazy {
        scheduleRepository.getSchedule(scheduleId)
    }

    val schedule
        get() = scheduleLiveData.value!!

    val isNumeratorLiveData: LiveData<Boolean> by lazy {
        MediatorLiveData<Boolean>().apply {
            value = true
            addSource(scheduleLiveData) { schedule ->
                schedule ?: return@addSource
                value = schedule.isNumeratorOnDate(LocalDateTime.now(), dayToSwitchToNextWeekOn)
            }
        }
    }

    val isNumerator
        get() = isNumeratorLiveData.value!!

    val subgroupLiveData: LiveData<Int> by lazy {
        MediatorLiveData<Int>().apply {
            value = 1
            addSource(scheduleLiveData) { schedule ->
                schedule ?: return@addSource
                value = schedule.subgroup
            }
        }
    }

    val subgroup
        get() = subgroupLiveData.value!!

    val classesListLiveData: LiveData<List<EntityClassWithSubject>> by lazy {
        scheduleRepository.getClassesWithSubjects(scheduleId, DayOfWeek.values()[dayOfWeekId])
    }

    //val classesList by delegateLiveData(classesListLiveData)

    fun updateSubjectSubgroup(newSubgroup: Int) {
        if (scheduleLiveData.isInitialized)
            viewModelScope.launch {
                scheduleRepository.updateSubgroup(schedule.id, newSubgroup)
            }
    }

    fun updateSubjectName(id: Long, newName: String?) = viewModelScope.launch {
        scheduleRepository.updateSubjectName(id, newName)
    }
}
