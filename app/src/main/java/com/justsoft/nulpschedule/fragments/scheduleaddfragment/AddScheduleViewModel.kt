package com.justsoft.nulpschedule.fragments.scheduleaddfragment

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justsoft.nulpschedule.model.InstituteAndGroup
import com.justsoft.nulpschedule.repo.ScheduleRepository
import com.justsoft.nulpschedule.utils.MutableStatefulLiveData
import com.justsoft.nulpschedule.utils.StatefulData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {
    val instituteAndGroupListLiveData: MutableStatefulLiveData<List<InstituteAndGroup>> =
        MutableStatefulLiveData(StatefulData.Uninitialized())

    init {
        getInstitutesAndGroups()
    }

    fun getInstitutesAndGroups() = viewModelScope.launch {
        scheduleRepository.getInstitutesAndGroups().observeForever {
            instituteAndGroupListLiveData.postValue(it)
        }
    }

    fun downloadSelectedScheduleAsync() = viewModelScope.async {
        with(selectedInstituteAndGroupLiveData.value!!) {
            scheduleRepository.downloadSchedule(institute, group, selectedSubgroupLiveData.value!!)
        }
    }

    val selectedInstituteAndGroupLiveData: MutableLiveData<InstituteAndGroup?> =
        MutableLiveData(null)
    val selectedSubgroupLiveData: MutableLiveData<Int?> = MutableLiveData(null)
    val shouldAddButtonBeEnabledLiveData = MediatorLiveData<Boolean>().apply {
        addSource(selectedInstituteAndGroupLiveData) {
            value = it != null && selectedSubgroupLiveData.value != null
        }
        addSource(selectedSubgroupLiveData) {
            value = it != null && selectedInstituteAndGroupLiveData.value != null
        }
    }
}