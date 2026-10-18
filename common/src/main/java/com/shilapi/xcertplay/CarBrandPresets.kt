package com.shilapi.xcertplay

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.shilapi.xcertplay.host.R
import java.io.ByteArrayOutputStream

/** Okavango-specific OEM logo preset. The source is an Android vector; PNG is generated on demand. */
data class CarBrandPreset(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val defaultOemName: String,
) {
    fun displayName(context: Context): String {
        val locale = androidx.core.os.ConfigurationCompat.getLocales(context.resources.configuration)[0]
        return if (locale?.language == "zh") "$nameZh ($nameEn)" else nameEn
    }

    fun loadBitmap(context: Context): Bitmap? = ContextCompat.getDrawable(
        context, R.drawable.ic_geely_auto_2023,
    )?.toBitmap(width = 512, height = 512, config = Bitmap.Config.ARGB_8888)

    /** Retains the original PNG byte contract expected by the CarPlay OEM branding settings. */
    fun loadBytes(context: Context): ByteArray? {
        val bitmap = loadBitmap(context) ?: return null
        return try {
            ByteArrayOutputStream().use { out ->
                if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) out.toByteArray() else null
            }
        } finally {
            bitmap.recycle()
        }
    }
}

object CarBrandPresets {
    val ALL: List<CarBrandPreset> = listOf(
        CarBrandPreset("geely", "吉利", "Geely", "Geely"),
    )
}
