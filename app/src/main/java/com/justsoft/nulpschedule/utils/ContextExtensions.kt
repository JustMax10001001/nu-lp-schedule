package com.justsoft.nulpschedule.utils

import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.view.inputmethod.InputMethodManager

val Context.clipboardManager: ClipboardManager
    get() = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

val Context.inputMethodManager: InputMethodManager
    get() = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

val Context.notificationManager: NotificationManager
    get() = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

