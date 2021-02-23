package com.justsoft.nulpschedule.fragments.dayviewfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.justsoft.nulpschedule.databinding.FragmentDayViewBinding
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.fragments.scheduleviewfragment.SharedDayViewFragmentViewModel
import com.justsoft.nulpschedule.utils.TimeFormatter
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
        mClassAdapter.subjectNameChange { subject, newName ->
            Firebase.analytics.logEvent("subject_name_change") { }
            sharedViewModel.updateSubjectName(subject.id, newName)
        }

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

    private fun updateSubjectList(
        newList: List<EntityClassWithSubject>?,
        newSubgroup: Int,
        newIsNumerator: Boolean
    ) {
        newList ?: return
        mClassAdapter.classList = newList.filter { subject ->
            subject.scheduleClass.classMatches(newSubgroup, newIsNumerator)
        }
        binding.suchEmptyClassesText.visibility =
            if (mClassAdapter.classList.isEmpty()) View.VISIBLE else View.GONE
    }
}