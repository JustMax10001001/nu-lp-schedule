package com.justsoft.nulpschedule.fragments.scheduleviewfragment

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class ScheduleViewFragmentViewModel @ViewModelInject constructor(
    @Assisted val savedStateHandle: SavedStateHandle
): ViewModel() {
    val scheduleId = savedStateHandle.getLiveData<Long>("schedule_id")
}