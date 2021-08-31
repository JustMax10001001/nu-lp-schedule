package com.justsoft.nulpschedule.fragments.dayviewfragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.databinding.FragmentDayViewBinding
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.fragments.scheduleviewfragment.SharedDayViewFragmentViewModel
import com.justsoft.nulpschedule.model.ScheduleClass
import com.justsoft.nulpschedule.model.Subject
import com.justsoft.nulpschedule.utils.AlertDialogExtensions
import com.justsoft.nulpschedule.utils.TimeFormatter
import com.justsoft.nulpschedule.utils.storeInClipboard
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DayViewFragment : Fragment() {

    private var _binding: FragmentDayViewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DayViewFragmentViewModel by viewModels()
    private val sharedViewModel: SharedDayViewFragmentViewModel by viewModels({ requireParentFragment() })

    @Inject
    lateinit var timeFormatter: TimeFormatter

    private lateinit var mClassAdapter: ClassRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentDayViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mClassAdapter = ClassRecyclerViewAdapter(requireContext(), timeFormatter)
        mClassAdapter.setHasStableIds(true)
        binding.daySubjectsRecyclerView.layoutManager = LinearLayoutManager(this.context)
        binding.daySubjectsRecyclerView.setHasFixedSize(true)
        binding.daySubjectsRecyclerView.setItemViewCacheSize(10)    // we don't really expect to have more than 10 classes

        binding.daySubjectsRecyclerView.adapter = mClassAdapter
        mClassAdapter.classContextAction(this::onClassAction)

        setUpObservers()
    }

    private fun setUpObservers() {
        viewModel.classesListLiveData.observe(this.viewLifecycleOwner) {
            updateSubjectList(
                it,
                sharedViewModel.subgroup,
                sharedViewModel.isNumerator
            )
        }
        sharedViewModel.subgroupLiveData.observe(this.viewLifecycleOwner) {
            updateSubjectList(
                viewModel.classesListLiveData.value,
                it,
                sharedViewModel.isNumerator
            )
        }
        sharedViewModel.isNumeratorLiveData.observe(this.viewLifecycleOwner) { isNumerator ->
            updateSubjectList(
                viewModel.classesListLiveData.value,
                sharedViewModel.subgroup,
                isNumerator
            )
        }
    }

    private fun updateSubjectName(subject: Subject, newName: String?) {
        Firebase.analytics.logEvent("subject_name_change") { }
        sharedViewModel.updateSubjectName(subject.id, newName)
    }

    private fun onClassAction(actionId: Int, classItem: EntityClassWithSubject): Boolean {
        when (actionId) {
            R.id.action_copy_class_url -> {
                classItem.scheduleClass.url?.let { url ->
                    copyUrlToClipboard(url)
                }
                return true
            }
            R.id.action_change_custom_subject_name -> {
                buildAndShowSubjectNameEditDialog(classItem.subject)
                return true
            }
            R.id.action_share_class -> {
                shareClass(classItem)
                return true
            }
            R.id.action_open_class_in -> {
                classItem.scheduleClass.url?.let { url -> openClassUrlIn(url) }
                return true
            }
            else -> Log.w("DayViewFragment", "Unhandled class action id")
        }
        return false
    }

    private fun copyUrlToClipboard(url: String) {
        requireContext().storeInClipboard(getString(R.string.online_class_url), url)
        Snackbar
            .make(requireView(), R.string.url_copied, Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun buildAndShowSubjectNameEditDialog(subjectToEdit: Subject) {
        AlertDialogExtensions.TextDialogBuilder(
            requireContext(),
            R.style.Theme_SchedulerTheme_AlertDialog
        )
            .setHint(R.string.custom_subject_name)
            .setText(subjectToEdit.customName ?: subjectToEdit.subjectName)
            .setTextInputListener(android.R.string.ok) { _, text ->
                val newCustomName = text.trim()

                if (newCustomName != subjectToEdit.customName && newCustomName.isNotEmpty())
                    updateSubjectName(subjectToEdit, newCustomName)
                else
                    updateSubjectName(subjectToEdit, null)
            }
            .setTitle(R.string.enter_custom_subject_name)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
            .show()
    }

    private fun shareClass(classWithSubject: EntityClassWithSubject) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/*"
        intent.putExtra(
            Intent.EXTRA_TEXT,
            buildClassShareText(classWithSubject.scheduleClass, classWithSubject.subject)
        )

        requireContext().startActivity(intent)
    }

    private fun buildClassShareText(
        scheduleClass: ScheduleClass,
        subject: Subject
    ): String = buildString {
        append(scheduleClass.index)
        append(". ")
        appendLine(subject.displayName)

        appendLine(scheduleClass.teacherName)

        appendLine(scheduleClass.classDescription)

        scheduleClass.url?.let {
            appendLine(requireContext().getString(R.string.link_format, it))
        }
    }

    private fun openClassUrlIn(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val chooser = Intent.createChooser(
            intent, requireContext().getString(R.string.open_lesson_in_app)
        )
        requireContext().startActivity(chooser)
    }

    private fun updateSubjectList(
        newList: List<EntityClassWithSubject>?,
        newSubgroup: Int,
        newIsNumerator: Boolean
    ) {
        newList ?: return
        mClassAdapter.updateDataSource(
            newList.filter { subject ->
                subject.scheduleClass.classMatches(newSubgroup, newIsNumerator)
            }
        )
        binding.suchEmptyClassesText.visibility =
            if (mClassAdapter.isEmpty()) View.VISIBLE else View.GONE
    }
}