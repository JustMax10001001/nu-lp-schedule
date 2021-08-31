package com.justsoft.nulpschedule.ui.recyclerview

import androidx.recyclerview.widget.RecyclerView

abstract class UpdatableEditableAdapter<Model, VH : RecyclerView.ViewHolder> :
    UpdatableAdapter<Model, VH>() {

    fun moveItem(from: Int, to: Int) {
        items.apply {
            val move = removeAt(from)
            add(to, move)
        }
        notifyItemMoved(from, to)
    }

    fun removeItemAt(adapterPosition: Int): Model {
        val removed = items.removeAt(adapterPosition)
        notifyItemRemoved(adapterPosition)
        return removed
    }
}