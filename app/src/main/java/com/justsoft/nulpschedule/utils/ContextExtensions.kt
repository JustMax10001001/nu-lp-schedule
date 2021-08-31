package com.justsoft.nulpschedule.utils

import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.*
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
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

fun Context.storeInClipboard(label: String, text: String) {
    clipboardManager.setPrimaryClip(
        ClipData.newPlainText(label, text)
    )
}

