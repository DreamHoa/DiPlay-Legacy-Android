package com.shilapi.xcertplay

import android.content.Context
import android.os.Build
import android.util.Log
import com.shilapi.xcertplay.orchestration.WirelessHotspotMode

/** Defaults for the observed Philippine E01. No radio, routing, vendor or firmware writes. */
object OkavangoProfile {
    private const val MARKER = "okavango_defaults_v1"

    fun matches(sdk: Int, model: String, hardware: String, device: String): Boolean =
        sdk == 22 && model.equals("E01", ignoreCase = true) &&
            hardware.equals("mt6735", ignoreCase = true) &&
            device.equals("VF12LQJ", ignoreCase = true)

    fun apply(context: Context) {
        if (!matches(Build.VERSION.SDK_INT, Build.MODEL, Build.HARDWARE, Build.DEVICE)) return
        val preferences = context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE)
        if (preferences.getBoolean(MARKER, false)) return
        val defaults: Map<String, Any> = linkedMapOf(
            "display_fps" to 30,
            // Retain the wide-screen aspect ratio; ~1286x482 on the 1920x720 panel.
            "display_scale_percent" to 67,
            "hevc_enabled" to false,
            "hevc_software_decoder" to false,
            "direct_surface_view" to true,
            "main_buffered_audio" to false,
            "smooth_video" to false,
            "wireless_hotspot_mode" to WirelessHotspotMode.EXISTING_WIFI.name,
            "manufacturer" to "Geely",
            "model" to "Okavango",
            "oem_label" to "GEELY",
        )
        val editor = preferences.edit()
        defaults.forEach { (key, value) ->
            if (!preferences.contains(key)) when (value) {
                is Int -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
            }
        }
        editor.putBoolean(MARKER, true).apply()
        // This independently signed test app has no compatible upstream release channel.
        val updates = context.getSharedPreferences("diplay_updates", Context.MODE_PRIVATE)
        if (!updates.contains("auto_check")) updates.edit().putBoolean("auto_check", false).apply()
        Log.i("DiPlay-Okavango", "E01/API22 defaults applied; saved user settings retained")
    }
}
