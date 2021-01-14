package com.justsoft.nulpschedule.fragments.scheduleselectfragment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.db.model.ScheduleTuple
import com.justsoft.nulpschedule.db.model.UpdateEntitySchedulePosition
import com.justsoft.nulpschedule.model.Schedule
import com.justsoft.nulpschedule.ui.recyclerview.AsyncLoadedViewHolder
import com.justsoft.nulpschedule.utils.TimeFormatter
import com.justsoft.nulpschedule.utils.lazyFind
import java.time.LocalDateTime
import kotlin.properties.Delegates

class ScheduleRecyclerViewAdapter(context: Context, private val timeFormatter: TimeFormatter) :
    RecyclerView.Adapter<ScheduleRecyclerViewAdapter.ScheduleViewHolder>() {

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
        return if (showCurrentClass && scheduleList[position].currentClass != null) {
            VIEW_TYPE_FULL
        } else {
            VIEW_TYPE_HALF
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val contentLayoutId = when (viewType) {
            VIEW_TYPE_FULL -> R.layout.schedule_content_card_layout
            VIEW_TYPE_HALF -> R.layout.schedule_content_card_half_layout
            else -> throw IllegalArgumentException("Unknown view type!")
        }
        val previewLayoutId = when (viewType) {
            VIEW_TYPE_FULL -> R.layout.schedule_background_card_layout
            VIEW_TYPE_HALF -> R.layout.schedule_background_card_half_layout
            else -> throw IllegalArgumentException("Unknown view type!")
        }
        return ScheduleViewHolder(parent.context, contentLayoutId) {
            mLayoutInflater.inflate(previewLayoutId, parent, false) as MaterialCardView
        }
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = scheduleList[position].schedule
        val currentClass = scheduleList[position].currentClass
        val nextClass = scheduleList[position].nextClass

        holder.invokeOnInflated {
            this as ScheduleViewHolder

            groupNameTextView.text = schedule.groupName
            instituteNameTextView.text = schedule.instituteName

            val showCurrentClass =
                currentClass != null && this@ScheduleRecyclerViewAdapter.showCurrentClass
            val context = holder.itemView.context

            if (showCurrentClass) {
                nowClass.subjectNameTextView.text = currentClass?.subject?.displayName
                nowClass.classEndTimeTextView.text = context.getString(
                    R.string.class_ends_at,
                    timeFormatter.formatEndTimeForSubjectIndex(
                        currentClass?.scheduleClass?.index ?: 1
                    )
                )
            }
            nextUpClass.classStartTimeTextView.text = context.getString(
                R.string.class_starts_at,
                timeFormatter.formatStartTimeForSubjectIndex(
                    nextClass?.scheduleClass?.index ?: 1
                )
            )
            nextUpClass.subjectNameTextView.text = nextClass?.subject?.displayName

            itemView.setOnClickListener {
                onSelectSchedule(schedule)
            }
        }
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
        initialLayoutFactory: (Context) -> ViewGroup
    ) : AsyncLoadedViewHolder(context, layoutId, initialLayoutFactory) {

        val groupNameTextView: TextView by itemView.lazyFind(R.id.group_text_view)
        val instituteNameTextView: TextView by itemView.lazyFind(R.id.institute_text_view)

        val nowClass = NowClass()

        val nextUpClass = NextClass()

        inner class NowClass {
            val subjectNameTextView: TextView by itemView.lazyFind(R.id.current_subject_name_text_view)
            val classEndTimeTextView: TextView by itemView.lazyFind(R.id.current_class_end_time_text_view)
        }

        inner class NextClass {
            val subjectNameTextView: TextView by itemView.lazyFind(R.id.next_subject_name_text_view)
            val classStartTimeTextView: TextView by itemView.lazyFind(R.id.next_class_start_time_text_view)
        }
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

    companion object {
        private const val VIEW_TYPE_HALF = 1
        private const val VIEW_TYPE_FULL = 2
    }
}