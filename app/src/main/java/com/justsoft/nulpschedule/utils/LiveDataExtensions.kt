package com.justsoft.nulpschedule.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class LiveDataDelegate<T>(
    private val liveData: LiveData<T>
) {
    operator fun getValue(dayViewFragmentViewModel: ViewModel, property: KProperty<*>): T {
        if (liveData.value == null)
            throw IllegalStateException("LiveData.value is not initialized yet!")
        return liveData.value!!
    }
}

fun <T> delegateLiveData(liveData: LiveData<T>): LiveDataDelegate<T> =
    LiveDataDelegate(liveData)

val LiveData<*>.isInitialized: Boolean
    get() = this.value != null

abstract class StatefulData<T> {
    class Success<T>(val data: T) : StatefulData<T>()
    class Error<T>(val throwable: Throwable) : StatefulData<T>()
    class Loading<T>(val loadingData: Any? = null) : StatefulData<T>()
    class Uninitialized<T>: StatefulData<T>()
}

typealias StatefulLiveData<T> = LiveData<StatefulData<T>>
typealias MutableStatefulLiveData<T> = MutableLiveData<StatefulData<T>>