package com.shilapi.xcertplay

import android.content.Context
import android.os.Build
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [22])
class OkavangoProfileTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val preferences get() = context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE)

    @Before fun targetHardware() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "E01")
        ReflectionHelpers.setStaticField(Build::class.java, "HARDWARE", "mt6735")
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "VF12LQJ")
        preferences.edit().clear().commit()
        context.getSharedPreferences("diplay_updates", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun selectsOnlyObservedHardware() {
        assertTrue(OkavangoProfile.matches(22, "E01", "mt6735", "VF12LQJ"))
        assertFalse(OkavangoProfile.matches(23, "E01", "mt6735", "VF12LQJ"))
        assertFalse(OkavangoProfile.matches(22, "E02", "mt6735", "VF12LQJ"))
        assertFalse(OkavangoProfile.matches(22, "E01", "mt6735", "FS11GQJ"))
    }

    @Test fun initializesLanAndLowerLoadWithoutChangingRadios() {
        OkavangoProfile.apply(context)
        assertEquals("EXISTING_WIFI", preferences.getString("wireless_hotspot_mode", null))
        assertEquals(30, preferences.getInt("display_fps", 0))
        assertEquals(67, preferences.getInt("display_scale_percent", 0))
        assertTrue(preferences.getBoolean("direct_surface_view", false))
        assertFalse(preferences.getBoolean("hevc_enabled", true))
        assertFalse(preferences.contains("manual_hotspot_ssid"))
        assertFalse(preferences.contains("existing_wifi_passphrase"))
    }

    @Test fun preservesSettingsAcrossFirstInstallMigrationAndRelaunch() {
        preferences.edit().putInt("display_fps", 15)
            .putString("wireless_hotspot_mode", "MANUAL").commit()
        OkavangoProfile.apply(context)
        assertEquals(15, preferences.getInt("display_fps", 0))
        assertEquals("MANUAL", preferences.getString("wireless_hotspot_mode", null))
        preferences.edit().putInt("display_scale_percent", 100).commit()
        OkavangoProfile.apply(context)
        assertEquals(100, preferences.getInt("display_scale_percent", 0))
    }

    @Test fun leavesOtherE01FirmwarePreferencesAlone() {
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "FS11GQJ")
        OkavangoProfile.apply(context)
        assertTrue(preferences.all.isEmpty())
    }
}
