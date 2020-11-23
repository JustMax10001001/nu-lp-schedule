package com.justsoft.nulpschedule.service.sync

import android.accounts.Account
import android.content.*
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import com.justsoft.nulpschedule.BuildConfig
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.repo.ScheduleRepository
import com.justsoft.nulpschedule.utils.notificationManager


class ScheduleSyncAdapter constructor(
    private val mScheduleRepository: ScheduleRepository,
    context: Context,
    autoInitialize: Boolean,
    allowParallelSyncs: Boolean = false
) : AbstractThreadedSyncAdapter(
    context, autoInitialize, allowParallelSyncs
) {

    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?
    ) {
        Log.d("SyncAdapter", "Syncing")
        mScheduleRepository.refreshAllSchedulesSync()
        Log.d("SyncAdapter", "Sync successful")
        if (BuildConfig.DEBUG) {
            val builder = NotificationCompat.Builder(context)
                .setContentTitle("DEBUG: background sync occurred")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
            context.notificationManager.notify(0, builder.build())
        }
    }
}