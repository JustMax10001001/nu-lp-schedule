package com.justsoft.nulpschedule.fragments.scheduleviewfragment

import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.justsoft.nulpschedule.fragments.dayviewfragment.DayViewFragment

class DayFragmentAdapter(fragment: Fragment, private val scheduleId: Long, private val switchDay: Int) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        Log.d("DayFragmentAdapter", "Creating fragment position $position")
        val dayFragment = DayViewFragment()

        dayFragment.arguments = bundleOf(
            "schedule_id" to scheduleId,
            "schedule_day" to position,
            "day_to_switch_to_next_week_on" to switchDay,
        )
        return dayFragment
    }
}