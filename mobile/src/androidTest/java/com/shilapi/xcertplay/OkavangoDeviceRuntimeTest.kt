package com.shilapi.xcertplay

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

/** Real E01 runtime checks; no Bluetooth switch, USB session or vendor maintenance. */
class OkavangoDeviceRuntimeTest {
    @Test fun deviceAndSavedDefaultsMatchObservedE01() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(22, Build.VERSION.SDK_INT)
        assertTrue(OkavangoProfile.matches(Build.VERSION.SDK_INT, Build.MODEL, Build.HARDWARE, Build.DEVICE))
        assertEquals("com.shihab.diplay.okavango", context.packageName)
        assertEquals(30, AirPlayPersistence.loadFps(context))
        assertEquals(67, AirPlayPersistence.loadDisplayScalePercent(context))
        assertFalse(AirPlayPersistence.loadHevcEnabled(context))
    }

    @Test fun pairingCryptoWorksOnE01Runtime() {
        T3LegacyRuntimeTest().pairingCryptoRoundTripsOnDalvik()
    }

    @Test fun selectedLocalAuthenticationLoadsAndSignsOnE01() {
        T3LegacyRuntimeTest().bundledAuthenticationLoadsAndSignsLocally()
    }

    @Test fun h264FrameActuallyDecodesOnE01() {
        T3LegacyRuntimeTest().h264FirstFrameRendersAndDecoderReleasesOnKitKat()
    }
}
