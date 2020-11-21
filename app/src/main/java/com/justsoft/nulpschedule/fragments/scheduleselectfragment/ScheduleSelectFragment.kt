package com.justsoft.nulpschedule.fragments.scheduleselectfragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.postDelayed
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.repo.RefreshState
import com.justsoft.nulpschedule.utils.ClassesTimetable
import com.justsoft.nulpschedule.ui.recyclerview.SwipeAndDragCallback
import com.justsoft.nulpschedule.utils.TimeFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_schedule_select.*
import kotlinx.android.synthetic.main.fragment_schedule_select.view.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleSelectFragment : Fragment() {

    private val viewModel: ScheduleSelectViewModel by viewModels()

    @Inject
    lateinit var classesTimetable: ClassesTimetable

    @Inject
    lateinit var timeFormatter: TimeFormatter

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
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeRecyclerView(view)
        setUpObservers()

        view.add_schedule_fab.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_addScheduleFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        updateTimeRunnable()
    }

    private fun setUpObservers() {
        viewModel.scheduleTupleListLiveData.observe(this.viewLifecycleOwner) {
            mScheduleRecyclerViewAdapter.scheduleList = it
        }
        viewModel.scheduleListLiveData.observe(this.viewLifecycleOwner) {
            such_empty_schedules_text.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun initializeRecyclerView(view: View) {
        mScheduleRecyclerViewAdapter = ScheduleRecyclerViewAdapter(timeFormatter)
        mScheduleRecyclerView = view.findViewById(R.id.schedule_select_recycler_view)

        mScheduleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        mScheduleRecyclerView.adapter = mScheduleRecyclerViewAdapter

        mSwipeAndDragHelper = createSwipeToDeleteHelper()
        mSwipeAndDragHelper.attachToRecyclerView(mScheduleRecyclerView)

        mScheduleRecyclerViewAdapter.selectSchedule {
            val arguments = Bundle()
            arguments.putLong("schedule_id", it.id)
            findNavController().navigate(
                R.id.action_FirstFragment_to_scheduleViewFragment,
                arguments
            )
        }

        mSwipeRefreshLayout = view.findViewById(R.id.schedule_selector_swipe_refresh)
        mSwipeRefreshLayout.setOnRefreshListener(this::onRefreshListener)
    }

    private fun onRefreshListener() {
        lifecycleScope.launch {
            viewModel.refreshSchedules().collect {
                Log.d("ScheduleSelectFragment", "onEach received $it")
                when (it) {
                    RefreshState.REFRESH_SUCCESS -> mSwipeRefreshLayout.isRefreshing = false
                    RefreshState.REFRESH_FAILED -> {
                        mSwipeRefreshLayout.isRefreshing = false
                        Snackbar.make(
                            mSwipeRefreshLayout,
                            R.string.something_went_wrong,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private fun createSwipeToDeleteHelper(): ItemTouchHelper =
        ItemTouchHelper(SwipeAndDragCallback(requireContext()).apply {
            this.delete { toDelete ->
                val removedIndex = toDelete.adapterPosition
                val removedSchedule =
                    mScheduleRecyclerViewAdapter.removeItemAt(toDelete.adapterPosition)

                viewModel.postScheduleForDeletion(removedSchedule.schedule)

                Snackbar.make(
                    mScheduleRecyclerView,
                    getString(R.string.schedule_is_removed),
                    Snackbar.LENGTH_LONG
                )
                    .setAnchorView(R.id.add_schedule_fab)
                    .setAction(getString(R.string.undo)) {
                        viewModel.cancelDeletion()
                        viewModel.scheduleTupleListLiveData.value?.let {
                            mScheduleRecyclerViewAdapter.scheduleList = it
                        }
                    }.show()
            }

            this.move { _, _ ->
                viewModel.updateSchedulePositions(mScheduleRecyclerViewAdapter.getSchedulePositions())
            }
        })

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