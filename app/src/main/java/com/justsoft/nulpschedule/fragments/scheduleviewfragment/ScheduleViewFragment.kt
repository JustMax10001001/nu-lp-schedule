package com.justsoft.nulpschedule.fragments.scheduleviewfragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

        val dayOfWeekTodayOrdinal = LocalDate.now().dayOfWeek.ordinal

        val dayFragmentAdapter = DayFragmentAdapter(this, viewModel.scheduleId.value!!)
        // Set fragment adapter to viewPager
        val dayViewPager = view.findViewById<ViewPager2>(R.id.subject_by_day_of_week_view_pager)
        dayViewPager.adapter = dayFragmentAdapter
        dayViewPager.setCurrentItem(
            when {
                dayOfWeekTodayOrdinal < 5 -> dayOfWeekTodayOrdinal
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

        menu.findItem(R.id.action_switch_numerator).actionView.apply {
            this as MaterialButton
            sharedViewModel.isNumeratorLiveData.observe(this@ScheduleViewFragment.viewLifecycleOwner) {
                this.isChecked = it ?: return@observe
            }

            setTooltipTextCompat(if (isChecked) R.string.switch_to_denominator else R.string.switch_to_numerator)
            addOnCheckedChangeListener { _, isChecked ->
                startAnimation(numeratorTurnAnimation)

                sharedViewModel.setNumeratorOverride(isChecked)
                setTooltipTextCompat(if (isChecked) R.string.switch_to_denominator else R.string.switch_to_numerator)
            }
        }

        menu.findItem(R.id.action_switch_subgroup).apply {
            sharedViewModel.subgroupLiveData.observe(this@ScheduleViewFragment.viewLifecycleOwner) {
                title = sharedViewModel.subgroup.toString()
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_switch_subgroup -> {
                sharedViewModel.updateSubjectSubgroup(
                    sharedViewModel.subgroup.and(1) + 1
                )          // https://imgur.com/a/Yssd6yl
                item.title = sharedViewModel.subgroup.toString()
                return true
            }
            R.id.action_switch_numerator -> {
                return true
            }
            R.id.action_schedule_details -> {
                showDetailsAlert()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showDetailsAlert() {
        AlertDialog.Builder(requireContext(), R.style.Theme_SchedulerTheme_AlertDialog)
            .setTitle(getString(R.string.schedule_details_alert_title))
            .setMessage(buildScheduleDetails())
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun buildScheduleDetails(): String {
        val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        return buildString {
            appendLine(
                getString(
                    R.string.added_on,
                    sharedViewModel.schedule.addTime.format(dateTimeFormatter)
                )
            )
            appendLine(
                getString(
                    R.string.last_updated_on,
                    sharedViewModel.schedule.updateTime.format(dateTimeFormatter)
                )
            )
        }
    }
}

