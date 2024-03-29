package com.justsoft.nulpschedule.fragments.scheduleviewfragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justsoft.nulpschedule.model.Schedule
import com.justsoft.nulpschedule.repo.ScheduleRepository
import com.justsoft.nulpschedule.utils.delegateLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class SharedDayViewFragmentViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val scheduleId: Long
        get() = savedStateHandle["schedule_id"]!!

    val scheduleLiveData: LiveData<Schedule> by lazy {
        scheduleRepository.getSchedule(scheduleId)
    }

    val schedule
        get() = scheduleLiveData.value!!

    val subjectCountLiveData: LiveData<Int> =
        scheduleRepository.getSubjectCountForSchedule(scheduleId)

    val subjectCount
        get() = subjectCountLiveData.value

    private val dayToSwitchToNextWeekOn: Int
        get() = savedStateHandle.get("day_to_switch_to_next_week_on") ?: 0

    private val isNumeratorTodayLiveData: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        value = true
        addSource(scheduleLiveData) { schedule ->
            schedule ?: return@addSource
            value = schedule.isNumeratorOnDate(LocalDateTime.now(), dayToSwitchToNextWeekOn)
        }
    }

    private val isNumeratorToday
        get() = isNumeratorTodayLiveData.value!!

    private val isNumeratorOverrideLiveData: MutableLiveData<Boolean> = MutableLiveData(null)

    private val isNumeratorOverride
        get() = isNumeratorOverrideLiveData.value

    val isNumeratorLiveData: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        value = isNumeratorToday
        addSource(isNumeratorTodayLiveData) {
            value = isNumeratorOverride ?: it
        }
        addSource(isNumeratorOverrideLiveData) {
            value = it ?: return@addSource
        }
    }

    val isNumerator by delegateLiveData(isNumeratorLiveData)

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

    fun updateSubjectSubgroup(newSubgroup: Int) {
        if (scheduleLiveData.isInitialized)
            viewModelScope.launch {
                scheduleRepository.updateSubgroup(schedule.id, newSubgroup)
            }
    }

    fun updateSubjectName(id: Long, newName: String?) = viewModelScope.launch {
        scheduleRepository.updateSubjectName(id, newName)
    }

    fun setNumeratorOverride(value: Boolean) {
        isNumeratorOverrideLiveData.value = value
    }
}