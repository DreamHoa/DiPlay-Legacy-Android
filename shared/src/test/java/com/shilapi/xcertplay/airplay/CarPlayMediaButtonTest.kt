package com.shilapi.xcertplay.airplay

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarPlayMediaButtonTest {
    @Test
    fun dilink3CarPlayVoiceKeysOpenSiriOnlyThroughTheWheelKeyService() {
        assertTrue(CarPlayMediaButton.opensSiriWhileCarPlay(327))
        assertTrue(CarPlayMediaButton.opensSiriWhileCarPlay(328))
        assertFalse(CarPlayMediaButton.opensSiriWhileCarPlay(304))
        assertFalse(CarPlayMediaButton.opensSiri(327))
    }

    @Test
    fun theVoiceKeyOpensSiri() {
        // Recorded on DiLink 5.0: short press 304 (scan 290), long press 312 (scan 312).
        assertTrue(CarPlayMediaButton.opensSiri(304))
        assertTrue(CarPlayMediaButton.opensSiri(312))
        assertTrue(CarPlayMediaButton.opensSiri(KeyEvent.KEYCODE_VOICE_ASSIST))
        assertFalse(CarPlayMediaButton.opensSiri(KeyEvent.KEYCODE_MEDIA_NEXT))
    }

    @Test
    fun indicesMatchTheAdvertisedMediaHidReport() {
        // Media report usages: 0 none, 1 play, 2 pause, 3 play/pause, 4 next, 5 previous.
        assertEquals(3, CarPlayMediaButton.PLAY_PAUSE)
        assertEquals(4, CarPlayMediaButton.NEXT)
        assertEquals(5, CarPlayMediaButton.PREVIOUS)
    }
}
