package com.justsoft.nulpschedule.service.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Simple empty authenticator service as per
 * <a href="https://developer.android.com/training/sync-adapters/creating-authenticator">Android developers guide</a>
 */
class SimpleAuthenticatorService : Service() {

    // Instance field that stores the authenticator object
    private lateinit var mAuthenticator: SimpleAuthenticator

    override fun onCreate() {
        // Create a new authenticator object
        mAuthenticator = SimpleAuthenticator(this)
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    override fun onBind(intent: Intent?): IBinder = mAuthenticator.iBinder
}