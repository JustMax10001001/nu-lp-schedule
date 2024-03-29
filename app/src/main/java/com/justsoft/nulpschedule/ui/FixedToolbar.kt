package com.justsoft.nulpschedule.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isGone

// Fixes https://stackoverflow.com/questions/47231180/toolbar-animatelayoutchanges-strange-behavior
class FixedToolbar(context: Context, attrs: AttributeSet?) : Toolbar(context, attrs) {
    companion object {
        private val navButtonViewField = Toolbar::class.java.getDeclaredField("mNavButtonView")
            .also { it.isAccessible = true }
    }

    override fun setNavigationIcon(icon: Drawable?) {
        super.setNavigationIcon(icon)

        (navButtonViewField.get(this) as? View)?.isGone = (icon == null)
    }
}