package com.justsoft.nulpschedule.fragments.dayviewfragment

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.google.android.material.card.MaterialCardView
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.ui.recyclerview.AsyncLoadedViewHolder
import com.justsoft.nulpschedule.ui.recyclerview.UpdatableAdapter
import com.justsoft.nulpschedule.utils.TimeFormatter
import com.justsoft.nulpschedule.utils.lazyFind

class ClassRecyclerViewAdapter(context: Context, private val timeFormatter: TimeFormatter) :
    UpdatableAdapter<EntityClassWithSubject, ClassRecyclerViewAdapter.ClassViewHolder>() {

    private val mLayoutInflater = LayoutInflater.from(context)

    private var onClassContextAction: (Int, EntityClassWithSubject) -> Boolean = { _, _ -> true }

    /**
     * Action which is invoked when user clicks on class' context menu item
     * @param action - the callback which is supplied with action id and the class item on which
     * the action is performed on
     */
    fun classContextAction(action: (Int, EntityClassWithSubject) -> Boolean) {
        onClassContextAction = action
    }

    override fun getItemId(position: Int): Long {
        return items[position].scheduleClass.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        return ClassViewHolder(parent.context, R.layout.class_content_card_layout) {
            (mLayoutInflater.inflate(
                R.layout.class_background_card_layout,
                parent,
                false
            ) as MaterialCardView)
        }
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val currentClass = items[position].scheduleClass
        val currentSubject = items[position].subject

        val onlineClassUrl = currentClass.url

        holder.invokeOnInflated {
            this as ClassViewHolder

            classIndexTextView.text = currentClass.index.toString()
            classStartTimeTextView.text =
                timeFormatter.formatStartTimeForSubjectIndex(currentClass.index)
            classEndTimeTextView.text =
                timeFormatter.formatEndTimeForSubjectIndex(currentClass.index)

            subjectNameTextView.text = currentSubject.displayName
            lecturerNameTextView.text = currentClass.teacherName
            classDescriptionTextView.text = currentClass.classDescription
            bulletedAdditionalInfoTextView.text =
                createAdditionalInfoSpan(items[position], itemView.context)

            itemView.setOnClickListener {
                onClassContextAction(
                    R.id.action_open_class_in,
                    items[position]
                )
            }

            // we inflate menu there, so we might as well do it
            // later while having more responsive recycler view
            itemView.post {
                val popup =
                    prepareButtonPopup(itemView.context, verticalEllipsisImageButton, position)
                verticalEllipsisImageButton.setOnClickListener { popup.show() }
            }
        }
    }

    private fun prepareButtonPopup(
        context: Context,
        button: ImageButton,
        position: Int
    ): PopupMenu {
        val onlineClassUrl = items[position].scheduleClass.url

        return PopupMenu(context, button, Gravity.END).apply {
            inflate(R.menu.context_menu_class_card)

            menu.findItem(R.id.action_copy_class_url).isEnabled = onlineClassUrl != null
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                setForceShowIcon(true)
            }

            setOnMenuItemClickListener { selectedItem ->
                onClassContextAction(
                    selectedItem.itemId,
                    items[position]
                )
            }
        }
    }

    private fun createAdditionalInfoSpan(
        entity: EntityClassWithSubject,
        context: Context
    ): String {
        val builder = StringBuilder()
        val bulletChar = '•'
        var shouldAppendBullet = false

        if (!entity.scheduleClass.isWeekAgnostic) {
            builder.append(
                context.getString(
                    if (entity.scheduleClass.isNumerator) R.string.numerator
                    else R.string.denominator
                )
            )
            shouldAppendBullet = true
        }
        if (!entity.scheduleClass.isSubgroupAgnostic) {
            val str = context.getString(
                if (entity.scheduleClass.isForFirstSubgroup) R.string.subgroup_1
                else R.string.subgroup_2
            )
            if (shouldAppendBullet)
                builder.append(" $bulletChar ")
            builder.append(str)
            shouldAppendBullet = true
        }

        if (entity.scheduleClass.isOnline) {
            val str = context.getString(R.string.online)
            if (shouldAppendBullet)
                builder.append(" $bulletChar ")
            builder.append(str)
            //shouldAppendBullet = true
        }
        return builder.toString()
    }

    class ClassViewHolder(
        context: Context,
        @LayoutRes layoutId: Int,
        initialLayoutFactory: (Context) -> ViewGroup,
    ) : AsyncLoadedViewHolder(context, layoutId, initialLayoutFactory) {

        //val classCardView: MaterialCardView by itemView.lazyFind(R.id.class_card_view)

        val subjectNameTextView: TextView by itemView.lazyFind(R.id.subject_name_text_view)
        val classDescriptionTextView: TextView by itemView.lazyFind(R.id.class_description_text_view)
        val lecturerNameTextView: TextView by itemView.lazyFind(R.id.lecturer_name_text_view)
        val bulletedAdditionalInfoTextView: TextView by itemView.lazyFind(R.id.bulleted_additional_info_text_view)

        val verticalEllipsisImageButton: ImageButton by itemView.lazyFind(R.id.vertical_elipsis_button)

        val classIndexTextView: TextView by itemView.lazyFind(R.id.class_index_text_view)
        val classStartTimeTextView: TextView by itemView.lazyFind(R.id.class_start_time_text_view)
        val classEndTimeTextView: TextView by itemView.lazyFind(R.id.class_end_time_text_view)
    }

    override fun areItemsTheSame(
        oldItem: EntityClassWithSubject,
        newItem: EntityClassWithSubject
    ): Boolean {
        return oldItem.scheduleClass.index == newItem.scheduleClass.index
    }

    override fun areContentsTheSame(
        oldItem: EntityClassWithSubject,
        newItem: EntityClassWithSubject
    ): Boolean {
        val oldClass = oldItem.scheduleClass
        val newClass = newItem.scheduleClass
        return (oldClass.url == null) == (newClass.url == null) &&
                oldClass.classDescription == newClass.classDescription &&
                oldClass.index == newClass.index &&
                oldClass.flags == newClass.flags &&
                oldClass.teacherName == newClass.teacherName &&
                oldItem.subject.displayName == newItem.subject.displayName
    }

    fun interface ClassContextActionCallback {
        fun onAction(@IdRes actionId: Int, item: EntityClassWithSubject): Boolean
    }
}
