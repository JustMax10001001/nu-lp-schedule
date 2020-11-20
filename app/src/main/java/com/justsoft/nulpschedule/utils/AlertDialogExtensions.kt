package com.justsoft.nulpschedule.utils

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.justsoft.nulpschedule.R
import kotlinx.android.synthetic.main.alert_dialog_text_input_layout.view.*


class AlertDialogExtensions private constructor() {


    class TextDialogBuilder(context: Context, @StyleRes resId: Int) :
        AlertDialog.Builder(context, resId) {

        private val mTextInputLayout: TextInputLayout
        private val mTextInputEditText: TextInputEditText

        private var requestEditTextFocusOnShow = false
        private var selectAll = false

        init {
            val frame = LayoutInflater.from(context)
                .inflate(R.layout.alert_dialog_text_input_layout, null)
            super.setView(frame)
            mTextInputLayout = frame.text_input_layout
            mTextInputEditText = frame.text_input_edit_text
        }

        fun setText(text: CharSequence) = this.apply {
            mTextInputEditText.setText(text)
        }

        fun setHint(@StringRes resId: Int) = this.apply {
            mTextInputLayout.hint = context.getString(resId)
        }

        fun requestEditTextFocusOnShow(value: Boolean) = this.apply {
            requestEditTextFocusOnShow = value
            if (requestEditTextFocusOnShow)
                mTextInputEditText.post { mTextInputEditText.requestFocusAndKeyboard() }
        }

        fun selectTextOnShow(value: Boolean) = this.apply {
            selectAll = value
            if (selectAll)
                mTextInputEditText.post { mTextInputEditText.selectAll() }
        }

        fun setTextInputListener(
            @StringRes positiveButtonText: Int,
            listener: OnTextInputListener
        ) = this.apply {
            setPositiveButton(positiveButtonText) { dialog, _ ->
                listener.onTextInput(dialog, mTextInputEditText.text?.toString() ?: "")
            }
        }
    }

    fun interface OnTextInputListener {
        fun onTextInput(dialog: DialogInterface, input: String)
    }
}