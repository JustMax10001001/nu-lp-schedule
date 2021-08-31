package com.justsoft.nulpschedule.fragments.settingsfragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.justsoft.nulpschedule.*

class MainSettingsFragment : PreferenceFragmentCompat() {

    private var mVersionClickCount = 0
    private val mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val versionPreference = findPreference<Preference>(getString(R.string.key_app_version))
        versionPreference?.summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        val clickCountReset = Runnable { mVersionClickCount = 0 }
        versionPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            ++mVersionClickCount

            if (mVersionClickCount == EASTER_EGG_TRIGGER_COUNT) {
                mVersionClickCount = 0
                mHandler.removeCallbacks(clickCountReset)
                Toast.makeText(requireContext(), R.string.easteregg_text, Toast.LENGTH_LONG).show()
                Firebase.analytics.logEvent("easteregg_trigger", null)
            }
            if (mVersionClickCount == 1) {
                mHandler.postDelayed(clickCountReset, (EASTER_EGG_TRIGGER_COUNT - 1) * 250.toLong())
            }

            return@OnPreferenceClickListener true
        }

        val telegramPreference =
            findPreference<Preference>(getString(R.string.key_open_telegram_channel))
        telegramPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val link = getString(R.string.app_telegram_link)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            val chooser =
                Intent.createChooser(intent, getString(R.string.intent_title_open_telegram_channel))
            startActivity(chooser)
            return@OnPreferenceClickListener true
        }

        val switchToNextDayPreference = findPreference<ListPreference>(getString(R.string.key_schedule_switch_day))
        switchToNextDayPreference?.setSummaryProvider { preference ->
            preference as ListPreference
            getString(R.string.schedule_switch_day_summary, preference.entry)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    companion object {
        private const val EASTER_EGG_TRIGGER_COUNT = 7
    }
}