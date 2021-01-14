package com.justsoft.nulpschedule.fragments.scheduleaddfragment

import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.databinding.FragmentAddScheduleBinding
import com.justsoft.nulpschedule.utils.StatefulData.*
import com.justsoft.nulpschedule.utils.animationEnd
import com.justsoft.nulpschedule.utils.inputMethodManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class AddScheduleFragment : Fragment() {

    private var _binding: FragmentAddScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddScheduleViewModel by viewModels()

    private lateinit var mInstituteAndGroupArrayAdapter: InstituteAndGroupArrayAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentAddScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpSubgroupSelector()
        initializeAutoCompleteSearch(view)
        setUpObservers()
        showLoading()
    }

    private fun setUpSubgroupSelector() {
        binding.subgroupSelector.addOnButtonCheckedListener { _, _, _ ->
            viewModel.selectedSubgroupLiveData.postValue(
                when (binding.subgroupSelector.checkedButtonId) {
                    R.id.checkbutton_subgroup_1 -> 1
                    R.id.checkbutton_subgroup_2 -> 2
                    else -> null
                }
            )
        }
    }

    private fun showLoading() {
        binding.search.isEnabled = false
        binding.searchWrapper.isEnabled = false
        binding.checkbuttonSubgroup1.isEnabled = false
        binding.checkbuttonSubgroup2.isEnabled = false
        showLoadingCircle()
    }

    private fun hideLoading() {
        binding.search.isEnabled = true
        binding.searchWrapper.isEnabled = true
        binding.checkbuttonSubgroup1.isEnabled = true
        binding.checkbuttonSubgroup2.isEnabled = true
        hideLoadingCircle()
    }

    private fun showLoadingCircle() {
        with(binding.loadingCircle) {
            alpha = 0.0f
            visibility = View.VISIBLE
            animate()
                .alpha(1.0f)
                .start()
        }
    }

    private fun hideLoadingCircle() {
        with(binding.loadingCircle) {
            alpha = 1.0f
            visibility = View.VISIBLE
            animate()
                .alpha(0.0f)
                .animationEnd {
                    visibility = View.GONE
                }
                .start()
        }
    }

    private fun setUpObservers() {
        viewModel.instituteAndGroupListLiveData.observe(this.viewLifecycleOwner) {
            when (it) {
                is Success -> {
                    Log.d("AddScheduleFragment", "Fetched Institutes and Groups successfully")
                    hideLoading()
                    mInstituteAndGroupArrayAdapter.setAll(it.data)
                    mInstituteAndGroupArrayAdapter.filter.filter(null)
                    binding.search.requestFocus()
                }
                is Loading -> Log.d("AddScheduleFragment", "Fetching Institutes and Groups")
                is Error -> {
                    Log.d(
                        "AddScheduleFragment",
                        "Error fetching Institutes and Groups",
                        it.throwable
                    )
                    hideLoadingCircle()
                    val errorSnackbar = Snackbar.make(
                        binding.search,
                        getString(R.string.error_loading_schedules),
                        Snackbar.LENGTH_LONG
                    )
                    errorSnackbar.setAction(getString(R.string.retry)) {
                        errorSnackbar.dismiss()
                        viewModel.getInstitutesAndGroups()
                        showLoadingCircle()
                    }
                    errorSnackbar.show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_add_schedule, menu)
        menu.findItem(R.id.add_schedule_item).apply {
            viewModel.shouldAddButtonBeEnabledLiveData.observe(this@AddScheduleFragment.viewLifecycleOwner) {
                this.isEnabled = it
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_schedule_item -> {
                actionDownloadSchedule(item)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun actionDownloadSchedule(item: MenuItem) {
        showLoading()
        item.isEnabled = false
        val deferredResult = viewModel.downloadSelectedScheduleAsync()
        deferredResult.invokeOnCompletion {
            hideLoading()
            item.isEnabled = true
            if (it == null) {
                val result = deferredResult.getCompleted()

                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val switchDay = sharedPreferences.getInt(
                    getString(
                        R.string.key_schedule_switch_day
                    ), 0
                )
                val arguments = bundleOf(
                    "schedule_id" to result.getOrThrow(),
                    "day_to_switch_to_next_week_on" to switchDay
                )

                // log successful download
                Firebase.analytics.logEvent("add_schedule") {
                    param(
                        "schedule_institute",
                        viewModel.selectedInstituteAndGroupLiveData.value?.institute.toString()
                    )
                    param(
                        "schedule_group",
                        viewModel.selectedInstituteAndGroupLiveData.value?.group.toString()
                    )
                }

                findNavController()
                    .navigate(
                        R.id.action_addScheduleFragment_to_scheduleViewFragment,
                        arguments
                    )
            } else {
                if (it is SQLiteConstraintException) {
                    Snackbar.make(
                        binding.search,
                        getString(R.string.this_schedule_was_already_added),
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    val errorSnackbar = Snackbar.make(
                        binding.search,
                        getString(
                            R.string.error_downloading_schedule
                        ), Snackbar.LENGTH_LONG
                    )
                    errorSnackbar.setAction(getString(R.string.retry)) {
                        errorSnackbar.dismiss()
                        actionDownloadSchedule(item)
                    }
                    Log.e(
                        "AddScheduleFragment",
                        "Error downloading schedule",
                        it
                    )
                    errorSnackbar.show()
                }
            }
        }
    }

    private fun initializeAutoCompleteSearch(view: View) {
        mInstituteAndGroupArrayAdapter = InstituteAndGroupArrayAdapter(requireContext())

        binding.search.setAdapter(mInstituteAndGroupArrayAdapter)
        binding.search.isEnabled = false
        binding.search.post {
            mInstituteAndGroupArrayAdapter.filter.filter(null)
        }
        binding.search.setOnItemClickListener { _, _, position, _ ->
            val item = mInstituteAndGroupArrayAdapter.getItem(position)
            val trimmed = item.toString().substringBeforeLast("...")

            if (item.toString() != trimmed) {
                binding.search.text.clear()
                binding.search.text.insert(0, trimmed)
                binding.search.post {
                    binding.search.showDropDown()
                    mInstituteAndGroupArrayAdapter.filter.filter(trimmed)
                }
                viewModel.selectedInstituteAndGroupLiveData.postValue(null)
            } else {
                binding.search.post {
                    requireContext().inputMethodManager.hideSoftInputFromWindow(    // hide keyboard
                        view.windowToken,
                        0
                    )
                    binding.search.clearFocus()
                }
                viewModel.selectedInstituteAndGroupLiveData.postValue(item)
            }
        }
        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                s ?: return
                if (s.toString() != viewModel.selectedInstituteAndGroupLiveData.value.toString()) {
                    viewModel.selectedInstituteAndGroupLiveData.postValue(null)             // invalidate selected object
                }
            }
        })
    }
}