package com.justsoft.nulpschedule

import com.justsoft.nulpschedule.api.ScheduleApi
import com.justsoft.nulpschedule.utils.net.SSLSocketFactoryWithAdditionalKeyStores
import org.junit.Before
import org.junit.Test
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory

class ScheduleApiTest {

    private lateinit var scheduleApi: ScheduleApi

    @Before
    fun setUp() {
        scheduleApi = ScheduleApi(SSLSocketFactoryWithAdditionalKeyStores(nulpSslCertificateKeyStore))

        //ks.load(fis, "changeit".toCharArray())
        println("Curr dir: ${System.getProperty("user.dir")}")
    }

    private fun loadKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance("BKS")
        FileInputStream("/src/main/res/raw/lpnu_keystore.bks").use {
            keyStore.load(it, "lpnu-password".toCharArray())
        }
        return keyStore
    }

    private val nulpSslCertificateKeyStore by lazy {
        val ks = KeyStore.getInstance("JKS")
        ks.load(null, null)
        val factory = CertificateFactory.getInstance("X.509")
        val fis = FileInputStream("nulp_ua_certificate.cer")
        val cert = factory.generateCertificate(fis)
        ks.setCertificateEntry("nulp", cert)
        ks
    }

    @Test
    fun generalTest() {
        val result = scheduleApi.getSchedule("ІКНІ", "ПЗ-14")
        assert(result.isSuccess)
        val unboxedResult = result.getOrThrow()
        println("----SCHEDULE----")
        println(unboxedResult.schedule.toString())
        println("----SUBJECTS----")
        for (subject in unboxedResult.subjects) {
            println("\t$subject")
        }
        println("----CLASSES----")
        for (clazz in unboxedResult.classes) {
            println("\t$clazz")
        }
    }
}