package com.justsoft.nulpschedule.fragments.scheduleselectfragment

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.db.model.ScheduleTuple
import com.justsoft.nulpschedule.db.model.UpdateEntitySchedulePosition
import com.justsoft.nulpschedule.model.Schedule
import com.justsoft.nulpschedule.ui.recyclerview.AsyncLoadedViewHolder
import com.justsoft.nulpschedule.utils.TimeFormatter
import java.time.LocalDateTime
import kotlin.properties.Delegates

class ScheduleRecyclerViewAdapter(context: Context, private val timeFormatter: TimeFormatter) :
    RecyclerView.Adapter<ScheduleRecyclerViewAdapter.ScheduleViewHolder>() {

    private val VIEW_TYPE_HALF = 1
    private val VIEW_TYPE_FULL = 2

    var showCurrentClass: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var scheduleList: List<ScheduleTuple> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        notifyChanges(oldValue, newValue)
    }

    private var onSelectSchedule: (Schedule) -> Unit = { }

    fun selectSchedule(action: (Schedule) -> Unit) {
        onSelectSchedule = action
    }

    private val mLayoutInflater = LayoutInflater.from(context)

    override fun getItemViewType(position: Int): Int {
        return if (showCurrentClass && scheduleList[position].nextClass != null) {
            VIEW_TYPE_FULL
        } else {
            VIEW_TYPE_HALF
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        return ScheduleViewHolder(parent.context, R.layout.schedule_preview_layout) {
            mLayoutInflater.inflate(R.layout.card_layout, parent, false) as MaterialCardView
        }
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        /*val binding = DataBindingUtil.findBinding<SchedulePreviewLayoutBinding>(holder.itemView)
            ?: return
        binding.timeFormatter = timeFormatter
        binding.schedule = scheduleList[position].schedule
        binding.currentClass = scheduleList[position].currentClass
        binding.showCurrentClass = showCurrentClass && binding.currentClass != null
        binding.nextClass = scheduleList[position].nextClass
        holder.itemView.apply {
            setOnClickListener {
                onSelectSchedule(binding.schedule!!)
            }
        }*/
    }

    override fun getItemCount(): Int {
        return scheduleList.size
    }

    private fun notifyChanges(
        oldScheduleList: List<ScheduleTuple>,
        newScheduleList: List<ScheduleTuple>
    ) {
        val listCallback = ScheduleDiffCallback(oldScheduleList, newScheduleList)
        val diffResult = DiffUtil.calculateDiff(listCallback)

        diffResult.dispatchUpdatesTo(this)
    }

    fun removeItemAt(adapterPosition: Int): ScheduleTuple {
        val removed = scheduleList[adapterPosition]
        scheduleList = scheduleList.filter { it.scheduleId != removed.scheduleId }
        return removed
    }

    fun returnItemAtPosition(adapterPosition: Int, removedSchedule: ScheduleTuple) {
        scheduleList = mutableListOf<ScheduleTuple>().apply {
            addAll(scheduleList)
            add(adapterPosition, removedSchedule)
        }
    }

    fun move(from: Int, to: Int) {
        scheduleList = mutableListOf<ScheduleTuple>().apply {
            addAll(scheduleList)
            val move = removeAt(from)
            add(to, move)
        }
    }

    fun getSchedulePositions(): List<UpdateEntitySchedulePosition> =
        scheduleList.mapIndexed { index, scheduleTuple ->
            UpdateEntitySchedulePosition(
                scheduleTuple.scheduleId,
                index
            )
        }

    class ScheduleViewHolder(
        context: Context,
        @LayoutRes layoutId: Int,
        temporaryLayoutFactory: (Context) -> ViewGroup
    ) : AsyncLoadedViewHolder(context, layoutId, temporaryLayoutFactory) {


    }

    internal class ScheduleDiffCallback(
        private val old: List<ScheduleTuple>,
        private val new: List<ScheduleTuple>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return old.size
        }

        override fun getNewListSize(): Int {
            return new.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return old[oldItemPosition].scheduleId == new[newItemPosition].scheduleId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = old[oldItemPosition]
            val newItem = new[newItemPosition]
            // check all displayed info
            return oldItem.schedule.groupName == newItem.schedule.groupName &&
                    oldItem.schedule.instituteName == newItem.schedule.instituteName &&
                    oldItem.schedule.isNumeratorOnDate(LocalDateTime.now()) == newItem.schedule.isNumeratorOnDate(
                LocalDateTime.now()
            ) &&
                    oldItem.schedule.subgroup == newItem.schedule.subgroup &&
                    oldItem.currentClass?.subject?.displayName == newItem.currentClass?.subject?.displayName &&
                    oldItem.currentClass?.scheduleClass?.index == newItem.currentClass?.scheduleClass?.index &&
                    oldItem.nextClass?.subject?.displayName == newItem.nextClass?.subject?.displayName &&
                    oldItem.nextClass?.scheduleClass?.index == newItem.nextClass?.scheduleClass?.index
        }
    }
}