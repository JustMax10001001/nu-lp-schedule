package com.justsoft.nulpschedule.fragments.scheduleviewfragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.databinding.FragmentScheduleViewBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

@AndroidEntryPoint
class ScheduleViewFragment : Fragment() {

    private var _binding: FragmentScheduleViewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduleViewFragmentViewModel by viewModels()
    private lateinit var mTabLayoutMediator: TabLayoutMediator

    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Set fragment adapter to viewPager
        val switchDay =
            mSharedPreferences.getString(getString(R.string.key_schedule_switch_day), "0")
                ?.toInt() ?: 0
        val dayFragmentAdapter = DayFragmentAdapter(
            this,
            viewModel.scheduleId.value!!,
            switchDay
        )
        val dayViewPager = view.findViewById<ViewPager2>(R.id.subject_by_day_of_week_view_pager)
        dayViewPager.adapter = dayFragmentAdapter
        dayViewPager.offscreenPageLimit = 4

        // Initialize day tabs
        val shortDayNames = resources.getStringArray(R.array.days_of_week_short)
        mTabLayoutMediator =
        TabLayoutMediator(binding.dayTabLayout, binding.subjectByDayOfWeekViewPager) { tab, position ->
            tab.text = shortDayNames[position]
        }
        mTabLayoutMediator.attach()
        dayViewPager.post {
            val todayDayOfWeek = LocalDate.now().dayOfWeek.ordinal
            dayViewPager.setCurrentItem(
                when {
                    todayDayOfWeek < 5 -> todayDayOfWeek
                    else -> 0
                }, true
            )
        }
    }
}

