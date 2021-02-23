package com.justsoft.nulpschedule.fragments.dayviewfragment

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.repo.ScheduleRepository
import java.time.DayOfWeek

class DayViewFragmentViewModel @ViewModelInject constructor(
    private val scheduleRepository: ScheduleRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val dayOfWeekId: Int
        get() = savedStateHandle.get("schedule_day")!!

    val scheduleId: Long
        get() = savedStateHandle.get("schedule_id")!!

    val classesListLiveData: LiveData<List<EntityClassWithSubject>> by lazy {
        scheduleRepository.getClassesWithSubjects(scheduleId, DayOfWeek.values()[dayOfWeekId])
    }
}
