package com.justsoft.nulpschedule.fragments.settingsfragment

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.justsoft.nulpschedule.*

class MainSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val versionPreference = findPreference<Preference>(getString(R.string.key_app_version))
        versionPreference?.summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

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

        /*val syncPreference =
            findPreference<SwitchPreferenceCompat>(getString(R.string.key_refresh_schedules))
        syncPreference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { pref, value ->
                ContentResolver.setSyncAutomatically(
                    ACCOUNT,
                    if (BuildConfig.DEBUG) AUTHORITY_DEBUG else AUTHORITY_RELEASE,
                    value
                )
                return@OnPreferenceChangeListener true
            }*/
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
}