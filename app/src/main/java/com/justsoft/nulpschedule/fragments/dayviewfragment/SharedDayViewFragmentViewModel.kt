package com.justsoft.nulpschedule.fragments.dayviewfragment

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedDayViewFragmentViewModel @ViewModelInject constructor(): ViewModel() {

    var isNumeratorOverride: MutableLiveData<Boolean> = MutableLiveData(null)
}