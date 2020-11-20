package com.justsoft.nulpschedule.fragments.scheduleaddfragment

import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.snackbar.Snackbar
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.utils.StatefulData
import com.justsoft.nulpschedule.utils.StatefulData.*
import com.justsoft.nulpschedule.utils.inputMethodManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_add_schedule.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class AddScheduleFragment : Fragment() {

    private val viewModel: AddScheduleViewModel by viewModels()

    private lateinit var mAutoCompleteInstituteAndGroup: AppCompatAutoCompleteTextView
    private lateinit var mInstituteAndGroupArrayAdapter: InstituteAndGroupArrayAdapter
    private lateinit var mSubgroupSelector: MaterialButtonToggleGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_add_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpSubgroupSelector(view)
        initializeAutoCompleteSearch(view)
        setUpObservers()
        showLoading()
    }

    private fun setUpSubgroupSelector(view: View) {
        mSubgroupSelector = view.findViewById(R.id.subgroup_selector)

        mSubgroupSelector.addOnButtonCheckedListener { _, _, _ ->
            viewModel.selectedSubgroupLiveData.postValue(
                when (mSubgroupSelector.checkedButtonId) {
                    R.id.checkbutton_subgroup_1 -> 1
                    R.id.checkbutton_subgroup_2 -> 2
                    else -> null
                }
            )
        }
    }

    private fun showLoading() {
        mAutoCompleteInstituteAndGroup.isEnabled = false
        search_wrapper.isEnabled = false
        checkbutton_subgroup_1.isEnabled = false
        checkbutton_subgroup_2.isEnabled = false
        showLoadingCircle()
    }

    private fun hideLoading() {
        mAutoCompleteInstituteAndGroup.isEnabled = true
        search_wrapper.isEnabled = true
        checkbutton_subgroup_1.isEnabled = true
        checkbutton_subgroup_2.isEnabled = true
        hideLoadingCircle()
    }

    private fun showLoadingCircle() {
        loading_circle.visibility = View.VISIBLE
    }

    private fun hideLoadingCircle() {
        loading_circle.visibility = View.GONE
    }

    private fun setUpObservers() {
        viewModel.instituteAndGroupListLiveData.observe(this.viewLifecycleOwner) {
            when (it) {
                is Success -> {
                    Log.d("AddScheduleFragment", "Fetched Institutes and Groups successfully")
                    hideLoading()
                    mInstituteAndGroupArrayAdapter.setAll(it.data)
                    mInstituteAndGroupArrayAdapter.filter.filter(null)
                    mAutoCompleteInstituteAndGroup.requestFocus()
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
                        mAutoCompleteInstituteAndGroup,
                        getString(R.string.error_loading_schedules),
                        Snackbar.LENGTH_INDEFINITE
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
                val arguments = Bundle()
                arguments.putLong("schedule_id", result.getOrThrow())
                findNavController()
                    .navigate(
                        R.id.action_addScheduleFragment_to_scheduleViewFragment,
                        arguments
                    )
            } else {
                if (it is SQLiteConstraintException) {
                    Snackbar.make(
                        mAutoCompleteInstituteAndGroup,
                        getString(R.string.this_schedule_was_already_added),
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    val errorSnackbar = Snackbar.make(
                        mAutoCompleteInstituteAndGroup,
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
        mAutoCompleteInstituteAndGroup = view.findViewById(R.id.search)
        mInstituteAndGroupArrayAdapter = InstituteAndGroupArrayAdapter(requireContext())

        mAutoCompleteInstituteAndGroup.setAdapter(mInstituteAndGroupArrayAdapter)
        mAutoCompleteInstituteAndGroup.isEnabled = false
        mAutoCompleteInstituteAndGroup.post {
            mInstituteAndGroupArrayAdapter.filter.filter(null)
        }
        mAutoCompleteInstituteAndGroup.setOnItemClickListener { _, _, position, _ ->
            val item = mInstituteAndGroupArrayAdapter.getItem(position)
            val trimmed = item.toString().substringBeforeLast("...")

            if (item.toString() != trimmed) {
                mAutoCompleteInstituteAndGroup.text.clear()
                mAutoCompleteInstituteAndGroup.text.insert(0, trimmed)
                mAutoCompleteInstituteAndGroup.post {
                    mAutoCompleteInstituteAndGroup.showDropDown()
                    mInstituteAndGroupArrayAdapter.filter.filter(trimmed)
                }
                viewModel.selectedInstituteAndGroupLiveData.postValue(null)
            } else {
                mAutoCompleteInstituteAndGroup.post {
                    requireContext().inputMethodManager.hideSoftInputFromWindow(    // hide keyboard
                        view.windowToken,
                        0
                    )
                    mAutoCompleteInstituteAndGroup.clearFocus()
                }
                viewModel.selectedInstituteAndGroupLiveData.postValue(item)
            }
        }
        mAutoCompleteInstituteAndGroup.addTextChangedListener(object : TextWatcher {
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