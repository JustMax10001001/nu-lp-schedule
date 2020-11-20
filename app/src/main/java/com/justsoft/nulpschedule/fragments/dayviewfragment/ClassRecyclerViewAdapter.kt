package com.justsoft.nulpschedule.fragments.dayviewfragment

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.databinding.ClassViewLayoutBinding
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.model.Subject
import com.justsoft.nulpschedule.utils.AlertDialogExtensions
import com.justsoft.nulpschedule.utils.TimeFormatter
import com.justsoft.nulpschedule.utils.clipboardManager
import kotlin.properties.Delegates

class ClassRecyclerViewAdapter(private val timeFormatter: TimeFormatter) :
    RecyclerView.Adapter<ClassRecyclerViewAdapter.SubjectViewHolder>() {

    var classList: List<EntityClassWithSubject> by Delegates.observable(emptyList()) { _, oldValue, newValue ->
        notifyChanges(oldValue, newValue)
    }

    private var onSubjectNameChange: (Subject, String?) -> Unit = { _, _ -> }

    fun subjectNameChange(action: (Subject, String?) -> Unit) {
        onSubjectNameChange = action
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val binding = DataBindingUtil.inflate<ClassViewLayoutBinding>(
            LayoutInflater.from(parent.context),
            R.layout.class_view_layout,
            parent,
            false
        )

        return SubjectViewHolder(binding.root)
    }

    private fun notifyChanges(
        oldSubjectList: List<EntityClassWithSubject>,
        newSubjectList: List<EntityClassWithSubject>
    ) {
        val subjectListCallback = ClassesDiffCallback(oldSubjectList, newSubjectList)
        val diffResult = DiffUtil.calculateDiff(subjectListCallback)

        diffResult.dispatchUpdatesTo(this)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val binding = DataBindingUtil.getBinding<ClassViewLayoutBinding>(holder.itemView)
        binding?.classWithSubject = classList[position]
        binding?.timeFormatter = timeFormatter

        val onlineClassUrl = classList[position].scheduleClass.url
        holder.itemView.apply {
            setOnClickListener {
                onlineClassUrl?.let { openClassUrl(it, holder.itemView.context) }
            }
            setOnCreateContextMenuListener { menu, view, _ ->
                val menuInflater = MenuInflater(view.context)
                menuInflater.inflate(R.menu.context_menu_class_card, menu)

                val copyUrlItem = menu.findItem(R.id.action_copy_class_url)
                copyUrlItem.isEnabled = onlineClassUrl != null
                copyUrlItem.setOnMenuItemClickListener { _ ->
                    onlineClassUrl?.let { url ->
                        copyUrlToClipboard(url, holder.itemView.context)
                        Snackbar.make(holder.itemView, R.string.url_copied, Snackbar.LENGTH_SHORT)
                            .show()
                        return@setOnMenuItemClickListener true
                    }
                    return@setOnMenuItemClickListener false
                }

                val changeSubjectNameItem = menu.findItem(R.id.action_change_custom_subject_name)
                changeSubjectNameItem.setOnMenuItemClickListener {
                    buildAndShowSubjectNameEditDialog(context, classList[position].subject)
                    return@setOnMenuItemClickListener true
                }
            }
            setOnLongClickListener { cardView ->
                cardView.showContextMenu()
            }
        }
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
            .selectTextOnShow(true)
            .requestEditTextFocusOnShow(true)
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

    override fun getItemCount(): Int = classList.size

    class SubjectViewHolder(subjectView: View) :
        RecyclerView.ViewHolder(subjectView)

    internal class ClassesDiffCallback(
        private val oldClasses: List<EntityClassWithSubject>,
        private val newClasses: List<EntityClassWithSubject>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldClasses.size
        }

        override fun getNewListSize(): Int {
            return newClasses.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldClasses[oldItemPosition].scheduleClass.index == newClasses[newItemPosition].scheduleClass.index
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldClass = oldClasses[oldItemPosition].scheduleClass
            val oldSubject = oldClasses[oldItemPosition].subject
            val newClass = newClasses[newItemPosition].scheduleClass
            val newSubject = newClasses[newItemPosition].subject
            return (oldClass.url == null) == (newClass.url == null) &&
                    oldClass.classDescription == newClass.classDescription &&
                    oldClass.index == newClass.index &&
                    oldClass.flags == newClass.flags &&
                    oldClass.teacherName == newClass.teacherName &&
                    oldSubject.displayName == newSubject.displayName

        }

    }
}
