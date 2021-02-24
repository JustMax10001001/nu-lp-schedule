package com.justsoft.nulpschedule.fragments.dayviewfragment

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.LayoutRes
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.model.ScheduleClass
import com.justsoft.nulpschedule.model.Subject
import com.justsoft.nulpschedule.ui.recyclerview.AsyncLoadedViewHolder
import com.justsoft.nulpschedule.ui.recyclerview.UpdatableAdapter
import com.justsoft.nulpschedule.utils.AlertDialogExtensions
import com.justsoft.nulpschedule.utils.TimeFormatter
import com.justsoft.nulpschedule.utils.clipboardManager
import com.justsoft.nulpschedule.utils.lazyFind

class ClassRecyclerViewAdapter(context: Context, private val timeFormatter: TimeFormatter) :
    UpdatableAdapter<EntityClassWithSubject, ClassRecyclerViewAdapter.ClassViewHolder>() {

    private val mLayoutInflater = LayoutInflater.from(context)

    private var onSubjectNameChange: (Subject, String?) -> Unit = { _, _ -> }

    fun subjectNameChange(action: (Subject, String?) -> Unit) {
        onSubjectNameChange = action
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
                onlineClassUrl?.let { openClassUrl(it, itemView.context) }
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
                when (selectedItem.itemId) {
                    R.id.action_copy_class_url -> {
                        onlineClassUrl?.let { url ->
                            copyUrlToClipboard(url, context)
                            Snackbar.make(button, R.string.url_copied, Snackbar.LENGTH_SHORT)
                                .show()
                        }
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_change_custom_subject_name -> {
                        buildAndShowSubjectNameEditDialog(context, items[position].subject)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_share_class -> {
                        shareClass(position, context)
                        return@setOnMenuItemClickListener true
                    }
                }
                return@setOnMenuItemClickListener false
            }
        }
    }

    private fun shareClass(position: Int, context: Context) {
        val scheduleClass = items[position].scheduleClass
        val subject = items[position].subject

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/*"
        intent.putExtra(Intent.EXTRA_TEXT, buildClassShareText(scheduleClass, subject, context))

        context.startActivity(intent)
    }

    private fun buildClassShareText(
        scheduleClass: ScheduleClass,
        subject: Subject,
        context: Context
    ): String = buildString {
        append(scheduleClass.index)
        append(". ")
        appendLine(subject.displayName)

        appendLine(scheduleClass.teacherName)

        appendLine(scheduleClass.classDescription)

        scheduleClass.url?.let {
            appendLine(context.getString(R.string.link_format, it))
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

    private fun buildAndShowSubjectNameEditDialog(context: Context, subjectToEdit: Subject) {
        AlertDialogExtensions.TextDialogBuilder(context, R.style.Theme_SchedulerTheme_AlertDialog)
            .setHint(R.string.custom_subject_name)
            .setText(subjectToEdit.customName ?: subjectToEdit.subjectName)
            .setTextInputListener(android.R.string.ok) { _, text ->
                val newCustomName = text.trim()

                if (newCustomName != subjectToEdit.customName && newCustomName.isNotEmpty())
                    onSubjectNameChange(subjectToEdit, newCustomName)
                else
                    onSubjectNameChange(subjectToEdit, null)
            }
            .setTitle(R.string.enter_custom_subject_name)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
            .show()
    }

    private fun copyUrlToClipboard(url: String, context: Context) {
        context.clipboardManager.setPrimaryClip(
            ClipData.newPlainText(
                context.getString(R.string.online_class_url), url
            )
        )
    }

    private fun openClassUrl(url: String, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val chooser = Intent.createChooser(intent, context.getString(R.string.open_lesson_in_app))
        context.startActivity(chooser)
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
}
