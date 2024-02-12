package com.justsoft.nulpschedule.fragments.dayviewfragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.repo.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class DayViewFragmentViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val dayOfWeekId: Int
        get() = savedStateHandle["schedule_day"]!!

    val scheduleId: Long
        get() = savedStateHandle["schedule_id"]!!

    val classesListLiveData: LiveData<List<EntityClassWithSubject>> by lazy {
        scheduleRepository.getClassesWithSubjects(scheduleId, DayOfWeek.values()[dayOfWeekId])
    }
}
