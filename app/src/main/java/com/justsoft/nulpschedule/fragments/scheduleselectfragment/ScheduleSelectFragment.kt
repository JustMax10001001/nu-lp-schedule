package com.justsoft.nulpschedule.fragments.scheduleselectfragment

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.databinding.FragmentScheduleSelectBinding
import com.justsoft.nulpschedule.model.RefreshState
import com.justsoft.nulpschedule.ui.recyclerview.SwipeAndDragCallback
import com.justsoft.nulpschedule.utils.ClassesTimetable
import com.justsoft.nulpschedule.utils.TimeFormatter
import com.justsoft.nulpschedule.utils.launch
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleSelectFragment : Fragment() {

    private var _binding: FragmentScheduleSelectBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<ScheduleSelectViewModel>()

    @Inject
    lateinit var classesTimetable: ClassesTimetable

    @Inject
    lateinit var timeFormatter: TimeFormatter

    private var mRefreshErrorSnackbar: Snackbar? = null
    private var mCancelDeletionSnackbar: Snackbar? = null

    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var mScheduleRecyclerView: RecyclerView

    private lateinit var mScheduleRecyclerViewAdapter: ScheduleRecyclerViewAdapter

    private lateinit var mSwipeAndDragHelper: ItemTouchHelper
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startUpdateTimeHandler()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        initializeRecyclerView(view)
        setUpObservers()

        binding.addScheduleFab.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_addScheduleFragment)
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            viewModel.flushDeletions()
        }
        mRefreshErrorSnackbar?.dismiss()
        mCancelDeletionSnackbar?.dismiss()
    }

    override fun onResume() {
        super.onResume()
        updateTimeRunnable()
        viewModel.invalidateScheduleList()
    }

    private fun setUpObservers() {
        viewModel.scheduleTupleListLiveData.observe(this.viewLifecycleOwner) {
            mScheduleRecyclerViewAdapter.updateDataSource(it)
        }
        viewModel.scheduleListLiveData.observe(this.viewLifecycleOwner) {
            binding.suchEmptySchedulesText.visibility =
                if (it.isEmpty()) View.VISIBLE else View.GONE
        }
        mSharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            when (key) {
                getString(R.string.key_show_current_class) -> {
                    mScheduleRecyclerViewAdapter.showCurrentClass =
                        mSharedPreferences.getBoolean(key, true)
                }
            }
        }
    }

    private fun initializeRecyclerView(view: View) {
        mScheduleRecyclerView = view.findViewById(R.id.schedule_select_recycler_view)
        mScheduleRecyclerViewAdapter = ScheduleRecyclerViewAdapter(requireContext(), timeFormatter)
        mScheduleRecyclerViewAdapter.showCurrentClass =
            mSharedPreferences.getBoolean(getString(R.string.key_show_current_class), true)

        mScheduleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        mScheduleRecyclerView.adapter = mScheduleRecyclerViewAdapter

        mSwipeAndDragHelper = createSwipeToDeleteHelper()
        mSwipeAndDragHelper.attachToRecyclerView(mScheduleRecyclerView)

        mScheduleRecyclerViewAdapter.selectSchedule {
            val arguments = Bundle()
            arguments.putLong("schedule_id", it.id)
            val switchDay =
                mSharedPreferences.getString(getString(R.string.key_schedule_switch_day), "0")
                    ?.toInt() ?: 0
            arguments.putInt("day_to_switch_to_next_week_on", switchDay)
            findNavController().navigate(
                R.id.action_FirstFragment_to_scheduleViewFragment,
                arguments
            )
        }

        mSwipeRefreshLayout = view.findViewById(R.id.schedule_selector_swipe_refresh)
        mSwipeRefreshLayout.setOnRefreshListener { onRefreshListener() }
    }

    private fun onRefreshListener() = launch {
        viewModel.refreshSchedules().collect {
            when (it) {
                RefreshState.PREPARING -> Log.d("ScheduleSelectFragment", "Preparing refresh")
                RefreshState.REFRESH_SUCCESS -> mSwipeRefreshLayout.isRefreshing = false
                RefreshState.REFRESH_FAILED -> {
                    mSwipeRefreshLayout.isRefreshing = false
                    makeRefreshErrorSnackBar()
                }
            }
        }
    }

    private fun makeRefreshErrorSnackBar() {
        Snackbar.make(
            requireView(),
            R.string.something_went_wrong,
            Snackbar.LENGTH_SHORT
        ).setAction(getString(R.string.retry)) {
            onRefreshListener()
            mRefreshErrorSnackbar = null
        }.also {
            mRefreshErrorSnackbar = it
        }.show()
    }

    private fun createSwipeToDeleteHelper(): ItemTouchHelper =
        ItemTouchHelper(SwipeAndDragCallback(requireContext()).apply {
            this.delete { toDelete ->
                val removedSchedule =
                    mScheduleRecyclerViewAdapter.removeItemAt(toDelete.adapterPosition)

                viewModel.postScheduleForDeletion(removedSchedule.schedule)

                makeCancelDeletionSnackBar()
                Firebase.analytics.logEvent("remove_schedule") { }
            }

            this.move { _, _ ->
                viewModel.updateSchedulePositions(mScheduleRecyclerViewAdapter.getSchedulePositions())
            }
        })

    private fun makeCancelDeletionSnackBar() {
        Snackbar.make(
            requireView(),
            getString(R.string.schedule_is_removed),
            Snackbar.LENGTH_LONG
        )
            .setAnchorView(R.id.add_schedule_fab)
            .setAction(getString(R.string.undo)) {
                viewModel.cancelDeletion()
                viewModel.scheduleTupleListLiveData.value?.let {
                    mScheduleRecyclerViewAdapter.updateDataSource(it)
                }
                mCancelDeletionSnackbar = null
            }.also {
                mCancelDeletionSnackbar = it
            }.show()
    }

    private val mUpdateTimeHandler = Handler(Looper.getMainLooper())

    private fun startUpdateTimeHandler() {
        val localDateTime = LocalDateTime.now()
        mUpdateTimeHandler.post(this::updateTimeRunnable)
        mUpdateTimeHandler.postDelayed({
            updateTimeRunnable()
            mUpdateTimeHandler.postDelayed(this::updateTimeRunnable, 60000)
        }, (60 - localDateTime.second) * 1000.toLong())
    }

    private fun updateTimeRunnable() {
        val localDateTime = LocalDateTime.now()
        val minutesDay = localDateTime.hour * 60 + localDateTime.minute
        val classIndex = classesTimetable.findCurrentClassIndex(minutesDay)
        val nextClassIndex = classesTimetable.findNextClassIndex(minutesDay)
        val dayOfWeek = localDateTime.dayOfWeek
        with(viewModel) {
            if (this.currentClassIndex != classIndex)           // skip redundant updates
                currentClassIndexLiveData.postValue(classIndex)
            if (this.nextClassIndex != nextClassIndex)
                nextClassIndexLiveData.postValue(nextClassIndex)
            if (this.currentDayOfWeek != dayOfWeek)
                currentDayOfWeekLiveData.postValue(dayOfWeek)
        }
    }
}
