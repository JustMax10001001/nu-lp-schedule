package com.justsoft.nulpschedule.fragments.dayviewfragment

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.model.Schedule
import com.justsoft.nulpschedule.repo.ScheduleRepository
import com.justsoft.nulpschedule.utils.delegateLiveData
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

    val scheduleLiveData: LiveData<Schedule> = scheduleRepository.getSchedule(scheduleId)
    val schedule by delegateLiveData(scheduleLiveData)

    val isNumeratorLiveData: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        value = true
        addSource(scheduleLiveData) { schedule ->
            schedule ?: return@addSource
            value = schedule.isNumeratorOnDate(LocalDateTime.now())
        }
    }

    val isNumerator by delegateLiveData(isNumeratorLiveData)

    val subgroupLiveData: LiveData<Int> = MediatorLiveData<Int>().apply {
        value = 1
        addSource(scheduleLiveData) {schedule ->
            schedule ?: return@addSource
            value = schedule.subgroup
        }
    }

    val subgroup by delegateLiveData(subgroupLiveData)

    val classesListLiveData: LiveData<List<EntityClassWithSubject>> =
        scheduleRepository.getClassesWithSubjects(scheduleId, DayOfWeek.values()[dayOfWeekId])

    val classesList by delegateLiveData(classesListLiveData)

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
