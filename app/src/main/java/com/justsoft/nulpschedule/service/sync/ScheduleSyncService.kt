package com.justsoft.nulpschedule.service.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.justsoft.nulpschedule.repo.ScheduleRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleSyncService : Service() {

    @Inject
    lateinit var mScheduleRepository: ScheduleRepository

    override fun onCreate() {
        super.onCreate()        // IMPORTANT: Hilt injects fields in super.onCreate()
        if (sSyncAdapter == null) {
            synchronized(sSyncAdapterLock) {
                sSyncAdapter = sSyncAdapter ?: ScheduleSyncAdapter(
                    mScheduleRepository,
                    applicationContext,
                    true
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d("SyncService", "onBind()")
        return sSyncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        private var sSyncAdapter: ScheduleSyncAdapter? = null
        private val sSyncAdapterLock = Any()
    }
}