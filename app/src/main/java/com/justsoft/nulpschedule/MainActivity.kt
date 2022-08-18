package com.justsoft.nulpschedule

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ActivityNavigator
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.justsoft.nulpschedule.ui.FixedToolbar
import dagger.hilt.android.AndroidEntryPoint

const val AUTHORITY_RELEASE = "com.justsoft.nulpschedule.service.sync.StubContentProvider"
const val AUTHORITY_DEBUG = "com.justsoft.nulpschedule.beta.service.sync.StubContentProvider"

// An account type, in the form of a domain name
const val ACCOUNT_TYPE = "nulpschedule.justsoft.com"

// The account name
const val ACCOUNT = "ScheduleSyncAccount"

const val SECONDS_PER_MINUTE = 60L
const val SYNC_INTERVAL_IN_MINUTES = 180L
const val SYNC_INTERVAL = SYNC_INTERVAL_IN_MINUTES * SECONDS_PER_MINUTE

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var mAccount: Account
    private lateinit var mResolver: ContentResolver
    private val mAuthority = if (BuildConfig.DEBUG) AUTHORITY_DEBUG else AUTHORITY_RELEASE

    private lateinit var mSharedPreferences: SharedPreferences
    private lateinit var mPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val toolbar = findViewById<FixedToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mResolver = contentResolver
        mAccount = createSyncAccount()

        ContentResolver.addPeriodicSync(
            mAccount,
            mAuthority,
            Bundle.EMPTY,
            if (BuildConfig.DEBUG) SYNC_INTERVAL / 9 else SYNC_INTERVAL
        )

        mPreferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener(this::preferenceChangeListener)
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener)

        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        NavigationUI.setupActionBarWithNavController(this, navController)
    }

    private fun preferenceChangeListener(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.key_refresh_schedules)) {
            Log.d("MainActivity", "Preference change listener for sync")
            ContentResolver.setSyncAutomatically(
                mAccount,
                mAuthority,
                sharedPreferences.getBoolean(key, true)
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.action_FirstFragment_to_mainSettingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Create a new placeholder account for the sync adapter
     */
    private fun createSyncAccount(): Account {
        val accountManager = getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
        return Account(ACCOUNT, ACCOUNT_TYPE).also { newAccount ->
            try {
                if (!accountManager.addAccountExplicitly(newAccount, null, null)) {
                    Log.w("MainActivity", "Could not add account, maybe it already exists")
                } else {
                    ContentResolver.setSyncAutomatically(newAccount, mAuthority, true)
                }
            } catch (e: SecurityException) {
                Log.w("MainActivity", "Could not add account", e)

            }

        }
    }

    override fun finish() {
        super.finish()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        return Navigation.findNavController(this, R.id.nav_host_fragment).navigateUp()
    }
}