package com.justsoft.nulpschedule.fragments.dayviewfragment

import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.databinding.FragmentDayViewBinding
import com.justsoft.nulpschedule.db.model.EntityClassWithSubject
import com.justsoft.nulpschedule.utils.TimeFormatter
import com.justsoft.nulpschedule.utils.setTooltipTextCompat
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

    private val numeratorTurnAnimation by lazy {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_180
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true)
        _binding = FragmentDayViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.daySubjectsRecyclerView.layoutManager = LinearLayoutManager(this.context)
        mClassAdapter = ClassRecyclerViewAdapter(timeFormatter)
        binding.daySubjectsRecyclerView.adapter = mClassAdapter
        mClassAdapter.subjectNameChange { subject, newName ->
            viewModel.updateSubjectName(subject.id, newName)
        }

        setUpObservers()
    }

    private fun setUpObservers() {
        viewModel.classesListLiveData.observe(owner = this.viewLifecycleOwner) {
            updateSubjectList(it, viewModel.subgroup, viewModel.isNumerator)
        }
        viewModel.subgroupLiveData.observe(owner = this.viewLifecycleOwner) {
            updateSubjectList(viewModel.classesListLiveData.value, it, viewModel.isNumerator)
        }
        viewModel.isNumeratorLiveData.observe(owner = this.viewLifecycleOwner) { isNumerator ->
            updateSubjectList(viewModel.classesListLiveData.value, viewModel.subgroup, isNumerator)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_schedule_view, menu)
        menu.findItem(R.id.switch_numerator).actionView.apply {
            this as MaterialButton
            viewModel.isNumeratorLiveData.observe(owner = this@DayViewFragment.viewLifecycleOwner) {
                this.isChecked = sharedViewModel.isNumeratorOverride.value ?: it
            }
            sharedViewModel.isNumeratorOverride.observe(owner = this@DayViewFragment.viewLifecycleOwner) {
                it?.let {
                    this.isChecked = it
                    updateSubjectList(
                        viewModel.classesListLiveData.value,
                        viewModel.subgroup,
                        it
                    )
                }
            }

            isChecked = sharedViewModel.isNumeratorOverride.value ?: viewModel.isNumerator
            setTooltipTextCompat(if (isChecked) R.string.switch_to_denominator else R.string.switch_to_numerator)
            addOnCheckedChangeListener { _, isChecked ->
                startAnimation(numeratorTurnAnimation)

                sharedViewModel.isNumeratorOverride.postValue(isChecked)
                updateSubjectList(
                    viewModel.classesListLiveData.value,
                    viewModel.subgroup,
                    isChecked
                )
                setTooltipTextCompat(if (isChecked) R.string.switch_to_denominator else R.string.switch_to_numerator)
            }
        }
        menu.findItem(R.id.switch_subgroup).apply {
            viewModel.subgroupLiveData.observe(owner = this@DayViewFragment.viewLifecycleOwner) {
                title = viewModel.subgroup.toString()
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.switch_subgroup -> {

                viewModel.updateSubjectSubgroup(
                    1 + viewModel.subgroup.and(1)
                )          // https://imgur.com/a/Yssd6yl
                item.title = viewModel.subgroup.toString()
                return true
            }
            R.id.switch_numerator -> {
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun updateSubjectList(
        newList: List<EntityClassWithSubject>?,
        newSubgroup: Int,
        newIsNumerator: Boolean
    ) {
        newList ?: return
        mClassAdapter.classList = newList.filter { subject ->
            with(subject.scheduleClass) {
                classMatches(
                    newSubgroup,
                    sharedViewModel.isNumeratorOverride.value ?: newIsNumerator
                )
            }
        }
        binding.suchEmptyClassesText.visibility =
            if (mClassAdapter.classList.isEmpty()) View.VISIBLE else View.GONE
    }
}