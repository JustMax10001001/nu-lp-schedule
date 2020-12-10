package com.justsoft.nulpschedule.service.sync

import android.accounts.Account
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import android.util.Log
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
        Log.d("SyncAdapter", "Syncing...")
        mScheduleRepository.refreshAllSchedules()
        Log.d("SyncAdapter", "Sync successful")
        if (BuildConfig.DEBUG) {
            @Suppress("DEPRECATION") val builder = Notification.Builder(context)
                .setContentTitle("DEBUG: background sync occurred")
                .setContentText("Success")
                .setSmallIcon(R.drawable.ic_launcher_schedule_foreground)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                builder.setChannelId("sync")
                val channel =
                    NotificationChannel("sync", "Sync", NotificationManager.IMPORTANCE_DEFAULT)
                context.notificationManager.createNotificationChannel(channel)
            }
            context.notificationManager.notify(100, builder.build())
        }
    }
}