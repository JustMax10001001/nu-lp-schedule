package com.justsoft.nulpschedule.utils

import android.content.Context
import com.justsoft.nulpschedule.R
import com.justsoft.nulpschedule.api.ScheduleApi
import com.justsoft.nulpschedule.utils.net.SSLSocketFactoryWithAdditionalKeyStores
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.KeyStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScheduleApiModule {
    @Provides
    @Singleton
    fun getScheduleApi(@ApplicationContext context: Context): ScheduleApi {
        return ScheduleApi(
            "https://student2023.lpnu.ua",
            SSLSocketFactoryWithAdditionalKeyStores(loadKeyStore(context))
        )
    }

    private fun loadKeyStore(context: Context): KeyStore {
        val keyStore = KeyStore.getInstance("BKS")
        context.resources.openRawResource(R.raw.lpnu_keystore).use {
            keyStore.load(it, context.getString(R.string.keystore_password).toCharArray())
        }
        return keyStore
    }
}