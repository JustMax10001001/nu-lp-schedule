package com.justsoft.nulpschedule.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.scopes.ServiceScoped

@AndroidEntryPoint
class ScheduleUpdateForegroundService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}