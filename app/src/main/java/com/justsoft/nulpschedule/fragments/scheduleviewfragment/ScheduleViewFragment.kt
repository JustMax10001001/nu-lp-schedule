package com.justsoft.nulpschedule.fragments.scheduleviewfragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayoutMediator
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.databinding.FragmentScheduleViewBinding
import com.justsoft.nulpschedule.utils.setTooltipTextCompat
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

@AndroidEntryPoint
class ScheduleViewFragment : Fragment() {

    private var _binding: FragmentScheduleViewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduleViewFragmentViewModel by viewModels()
    private val sharedViewModel: SharedDayViewFragmentViewModel by viewModels({ this })

    private lateinit var mTabLayoutMediator: TabLayoutMediator
    private lateinit var mSharedPreferences: SharedPreferences

    private lateinit var numeratorTurnAnimation: Animation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentScheduleViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        numeratorTurnAnimation = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_180
        )

        val dayOfWeekToday = LocalDate.now().dayOfWeek.ordinal

        val dayFragmentAdapter = DayFragmentAdapter(
            this,
            viewModel.scheduleId.value!!
        )
        // Set fragment adapter to viewPager
        val dayViewPager = view.findViewById<ViewPager2>(R.id.subject_by_day_of_week_view_pager)
        dayViewPager.adapter = dayFragmentAdapter
        dayViewPager.setCurrentItem(
            when {
                dayOfWeekToday < 5 -> dayOfWeekToday
                else -> 0
            },
            false
        )
        dayViewPager.offscreenPageLimit = 2

        // Initialize day tabs
        val shortDayNames = resources.getStringArray(R.array.days_of_week_short)
        mTabLayoutMediator =
            TabLayoutMediator(
                binding.dayTabLayout,
                binding.subjectByDayOfWeekViewPager
            ) { tab, position ->
                tab.text = shortDayNames[position]
            }
        mTabLayoutMediator.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_schedule_view, menu)

        menu.findItem(R.id.switch_numerator).actionView.apply {
            this as MaterialButton
            sharedViewModel.isNumeratorLiveData.observe(this@ScheduleViewFragment.viewLifecycleOwner) {
                this.isChecked = it ?: return@observe
            }

            //isChecked = sharedViewModel.isNumeratorOverride ?: sharedViewModel.isNumeratorToday
            setTooltipTextCompat(if (isChecked) R.string.switch_to_denominator else R.string.switch_to_numerator)
            addOnCheckedChangeListener { _, isChecked ->
                startAnimation(numeratorTurnAnimation)

                sharedViewModel.setNumeratorOverride(isChecked)
                setTooltipTextCompat(if (isChecked) R.string.switch_to_denominator else R.string.switch_to_numerator)
            }
        }

        menu.findItem(R.id.switch_subgroup).apply {
            sharedViewModel.subgroupLiveData.observe(this@ScheduleViewFragment.viewLifecycleOwner) {
                title = sharedViewModel.subgroup.toString()
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.switch_subgroup -> {
                sharedViewModel.updateSubjectSubgroup(
                    sharedViewModel.subgroup.and(1) + 1
                )          // https://imgur.com/a/Yssd6yl
                item.title = sharedViewModel.subgroup.toString()
                return true
            }
            R.id.switch_numerator -> {
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}

