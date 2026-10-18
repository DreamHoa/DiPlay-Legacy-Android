package com.shilapi.xcertplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

data class CarBrandPreset(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val defaultOemName: String,
    val assetFileName: String,
) {
    fun displayName(context: Context): String {
        val locale = androidx.core.os.ConfigurationCompat.getLocales(context.resources.configuration)[0]
        val isZh = locale?.language?.startsWith("zh") == true
        return if (isZh) "$nameZh ($nameEn)" else nameEn
    }

    fun loadBitmap(context: Context): Bitmap? = try {
        context.assets.open("car_brands/$assetFileName").use {
            BitmapFactory.decodeStream(it)
        }
    } catch (_: Exception) {
        null
    }

    fun loadBytes(context: Context): ByteArray? = try {
        context.assets.open("car_brands/$assetFileName").use {
            it.readBytes()
        }
    } catch (_: Exception) {
        null
    }
}

object CarBrandPresets {
    // Okavango is the only supported head unit; no cross-brand preset catalogue.
    val ALL: List<CarBrandPreset> = listOf(
        CarBrandPreset("geely", "吉利", "Geely", "Geely", "geely.png"),
    )
}
