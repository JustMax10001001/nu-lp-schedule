package com.justsoft.nulpschedule.fragments.scheduleviewfragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.justsoft.nulpschedule.fragments.dayviewfragment.DayViewFragment

class DayFragmentAdapter(fragment: Fragment, private val scheduleId: Long) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        Log.d("DayFragmentAdapter", "Creating fragment position $position")
        val dayFragment = DayViewFragment()
        dayFragment.arguments = Bundle().apply {
            putLong("schedule_id", scheduleId)
            putInt("schedule_day", position)
        }
        return dayFragment
    }
}