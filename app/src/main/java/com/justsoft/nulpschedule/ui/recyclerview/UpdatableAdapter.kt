package com.justsoft.nulpschedule.ui.recyclerview

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.justsoft.nulpschedule.utils.ListDiffCallback

/**
 * An adapter which is capable of updating its data source
 */
abstract class UpdatableAdapter<Model, VH : RecyclerView.ViewHolder> :
    RecyclerView.Adapter<VH>() {

    protected val items = mutableListOf<Model>()

    fun updateDataSource(source: List<Model>) {
        notifyChanges(items, source)
        items.clear()
        items.addAll(source)
    }

    fun isEmpty(): Boolean = items.isEmpty()

    override fun getItemCount(): Int = items.size

    private fun notifyChanges(
        oldSubjectList: List<Model>,
        newSubjectList: List<Model>
    ) {
        val listCallback = createDiffCallback(oldSubjectList, newSubjectList)
        val diffResult = DiffUtil.calculateDiff(listCallback)

        diffResult.dispatchUpdatesTo(this)
    }

    protected abstract fun areItemsTheSame(oldItem: Model, newItem: Model): Boolean

    protected abstract fun areContentsTheSame(oldItem: Model, newItem: Model): Boolean

    private fun createDiffCallback(oldList: List<Model>, newList: List<Model>) =
        object : ListDiffCallback<Model>(oldList, newList) {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                areItemsTheSame(oldList[oldItemPosition], newList[newItemPosition])

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                areContentsTheSame(oldList[oldItemPosition], newList[newItemPosition])
        }
}