package com.justsoft.nulpschedule.ui.recyclerview

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.fragments.scheduleselectfragment.ScheduleRecyclerViewAdapter

class SwipeAndDragCallback(
    context: Context
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN,
    ItemTouchHelper.LEFT
) {

    private val deleteIcon =
        ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_outline_32)!!
    private val intrinsicWidth = deleteIcon.intrinsicWidth
    private val intrinsicHeight = deleteIcon.intrinsicHeight

    private val background = GradientDrawable()
    private val backgroundColor = ContextCompat.getColor(context, R.color.red_900)
    private val backgroundCornerRadius =
        context.resources.getDimensionPixelSize(R.dimen.card_corner_radius)

    private lateinit var onDeleteCallback: (RecyclerView.ViewHolder) -> Unit
    private lateinit var onMoveCallback: OnMoveCallback

    fun move(onMoveCallback: OnMoveCallback) {
        this.onMoveCallback = onMoveCallback
    }

    fun delete(onDeleteCallback: (RecyclerView.ViewHolder) -> Unit) {
        this.onDeleteCallback = onDeleteCallback
    }

    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        (recyclerView.adapter as ScheduleRecyclerViewAdapter).move(
            viewHolder.adapterPosition,
            target.adapterPosition
        )
        onMoveCallback.onMove(viewHolder, target)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        if (direction == ItemTouchHelper.LEFT)
            onDeleteCallback(viewHolder)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(
                c,
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Draw the red delete background
        background.color = ColorStateList.valueOf(backgroundColor)
        backgroundCornerRadius.toFloat().let { r ->
            background.cornerRadii = floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
        }
        background.setBounds(
            itemView.right + dX.toInt() - backgroundCornerRadius,
            itemView.top,
            itemView.right,
            itemView.bottom
        )

        background.draw(c)

        // Calculate position of delete icon
        val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
        //val deleteIconMarginVertical = (itemHeight - intrinsicHeight) / 2
        val deleteIconMarginHorizontal = intrinsicWidth / 8 * 5
        val deleteIconLeft = itemView.right - deleteIconMarginHorizontal - intrinsicWidth
        val deleteIconRight = itemView.right - deleteIconMarginHorizontal
        val deleteIconBottom = deleteIconTop + intrinsicHeight

        // Draw the delete icon
        deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        deleteIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }

    fun interface OnMoveCallback {
        fun onMove(viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder)
    }
}
