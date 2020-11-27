package com.justsoft.nulpschedule.utils

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import com.justsoft.nulpschedule.databinding.AlertDialogTextInputLayoutBinding


class AlertDialogExtensions private constructor() {


    class TextDialogBuilder(context: Context, @StyleRes resId: Int) :
        AlertDialog.Builder(context, resId) {

        private var _binding: AlertDialogTextInputLayoutBinding? = null
        private val binding get() = _binding!!

        private var requestEditTextFocusOnShow = false
        private var selectAll = false

        init {
            _binding = AlertDialogTextInputLayoutBinding.inflate(
                LayoutInflater.from(context),
                null,
                false
            )
            super.setView(binding.root)
        }

        fun setText(text: CharSequence) = this.apply {
            binding.textInputEditText.setText(text)
        }

        fun setHint(@StringRes resId: Int) = this.apply {
            binding.textInputLayout.hint = context.getString(resId)
        }

        fun requestEditTextFocusOnShow(value: Boolean) = this.apply {
            requestEditTextFocusOnShow = value
            if (requestEditTextFocusOnShow)
                binding.textInputEditText.post { binding.textInputEditText.requestFocusAndKeyboard() }
        }

        fun selectTextOnShow(value: Boolean) = this.apply {
            selectAll = value
            if (selectAll)
                binding.textInputEditText.post { binding.textInputEditText.selectAll() }
        }

        fun setTextInputListener(
            @StringRes positiveButtonText: Int,
            listener: OnTextInputListener
        ) = this.apply {
            setPositiveButton(positiveButtonText) { dialog, _ ->
                listener.onTextInput(dialog, binding.textInputEditText.text?.toString() ?: "")
            }
        }
    }

    fun interface OnTextInputListener {
        fun onTextInput(dialog: DialogInterface, input: String)
    }
}