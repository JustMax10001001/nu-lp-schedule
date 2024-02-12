package com.justsoft.nulpschedule.fragments.scheduleviewfragment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ScheduleViewFragmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val scheduleId = savedStateHandle.getLiveData<Long>("schedule_id")
}