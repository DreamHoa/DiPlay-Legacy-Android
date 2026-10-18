package com.shilapi.xcertplay

import android.graphics.BitmapFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class CarBrandPresetsTest {
    @Test fun onlyOkavangoGeelyPresetUsesValidVectorBackedPng() {
        val preset = CarBrandPresets.ALL.single()
        assertEquals("geely", preset.id)
        val bytes = requireNotNull(preset.loadBytes(RuntimeEnvironment.getApplication()))
        assertTrue(bytes.size > 200)
        assertEquals(0x89, bytes[0].toInt() and 0xff)
        assertEquals(0x50, bytes[1].toInt() and 0xff)
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        assertEquals(512, bitmap.width)
        assertEquals(512, bitmap.height)
        bitmap.recycle()
    }
}
