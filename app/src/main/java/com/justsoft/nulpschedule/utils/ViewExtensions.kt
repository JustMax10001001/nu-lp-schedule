package com.justsoft.nulpschedule.utils

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat

fun View.requestFocusAndKeyboard() {
    this.requestFocus()
    context.inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.setTooltipTextCompat(@StringRes resId: Int) {
    TooltipCompat.setTooltipText(this, context.getString(resId))
}

fun <T: View> View.lazyFind(@IdRes viewId: Int) = lazy {
    this.findViewById<T>(viewId)
}