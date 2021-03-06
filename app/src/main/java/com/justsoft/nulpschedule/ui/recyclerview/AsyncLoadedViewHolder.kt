package com.justsoft.nulpschedule.ui.recyclerview

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.LayoutRes
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.recyclerview.widget.RecyclerView

open class AsyncLoadedViewHolder(
    context: Context,
    @LayoutRes layoutId: Int,
    initialLayoutFactory: (Context) -> ViewGroup
) : RecyclerView.ViewHolder(initialLayoutFactory(context)) {

    private val temporaryLayout: ViewGroup = itemView as ViewGroup

    private lateinit var onInflated: AsyncLoadedViewHolder.() -> Unit
    var isInflationComplete = false
        private set

    init {
        val asyncLayoutInflater = AsyncLayoutInflater(context)
        asyncLayoutInflater.inflate(
            layoutId,
            temporaryLayout
        ) { view, _, _ ->
            view.alpha = 0.1f
            temporaryLayout.addView(
                view,
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
            )
            view.animate()
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setDuration(250)
                .alpha(1f)
                .start()
            isInflationComplete = true
            if (this::onInflated.isInitialized) {
                onInflated()
            }
        }
    }

    fun invokeOnInflated(onInflated: AsyncLoadedViewHolder.() -> Unit) {
        if (isInflationComplete) {
            onInflated()
        } else {
            this.onInflated = onInflated
        }
    }
}