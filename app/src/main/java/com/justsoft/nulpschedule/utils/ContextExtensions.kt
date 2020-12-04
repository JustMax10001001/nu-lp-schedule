package com.justsoft.nulpschedule.utils

import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.*
import android.os.Build.*
import android.view.inputmethod.InputMethodManager

val Context.clipboardManager: ClipboardManager
    get() = if (VERSION.SDK_INT >= VERSION_CODES.M) {
        this.getSystemService(ClipboardManager::class.java)
    } else {
        this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }

val Context.inputMethodManager: InputMethodManager
    get() = if (VERSION.SDK_INT >= VERSION_CODES.M) {
        this.getSystemService(InputMethodManager::class.java)
    } else {
        this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    }

val Context.notificationManager: NotificationManager
    get() = if (VERSION.SDK_INT >= VERSION_CODES.M) {
        this.getSystemService(NotificationManager::class.java)
    } else {
        this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

