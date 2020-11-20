package com.justsoft.nulpschedule.utils

import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import com.google.android.material.button.MaterialButton
import com.justsoft.nulpschedule.R

fun View.requestFocusAndKeyboard() {
    this.requestFocus()
    context.inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.setTooltipTextCompat(@StringRes resId: Int) {
    TooltipCompat.setTooltipText(this, context.getString(resId))
}