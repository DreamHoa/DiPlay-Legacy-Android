package com.shilapi.xcertplay

import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import org.junit.Assert.*
import org.junit.Test

class GeelyWheelPressesTest {
    @Test fun realVendorDownUpPairsSendOneTrackCommandEach() {
        val keys = GeelyWheelPresses()
        val actual = listOf(163 to 1, 163 to 0, 163 to 1, 163 to 0, 165 to 1, 165 to 0, 165 to 1, 165 to 0)
            .mapNotNull { (key, action) -> keys.onKey(key, action) }
        assertEquals(listOf(CarPlayMediaButton.NEXT, CarPlayMediaButton.NEXT,
            CarPlayMediaButton.PREVIOUS, CarPlayMediaButton.PREVIOUS), actual)
    }

    @Test fun holdAndDuplicateDownPacketsDoNotSkipExtraTracks() {
        val keys = GeelyWheelPresses()
        assertEquals(CarPlayMediaButton.NEXT, keys.onKey(163, 1))
        assertNull(keys.onKey(163, 1))
        assertNull(keys.onKey(163, 2))
        assertNull(keys.onKey(163, 2))
        assertNull(keys.onKey(163, 0))
        assertNull(keys.onKey(163, 0))
        assertEquals(CarPlayMediaButton.NEXT, keys.onKey(163, 1))
    }

    @Test fun releaseOrOrphanedHoldDoesNotInventAPress() {
        val keys = GeelyWheelPresses()
        assertNull(keys.onKey(165, 0))
        assertNull(keys.onKey(165, 2))
        assertNull(keys.onKey(165, 1))
        assertNull(keys.onKey(165, 0))
        assertEquals(CarPlayMediaButton.PREVIOUS, keys.onKey(165, 1))
    }

    @Test fun unrelatedKeysAndInvalidActionsAreIgnored() {
        val keys = GeelyWheelPresses()
        for (key in listOf(87, 88, 113, 114, 115, 239, 269, 200087, 200088)) {
            for (action in 0..3) assertNull(keys.onKey(key, action))
        }
        assertNull(keys.onKey(163, 3))
        assertEquals(CarPlayMediaButton.NEXT, keys.onKey(163, 1))
    }

    @Test fun serviceRestartClearsHeldState() {
        val keys = GeelyWheelPresses()
        keys.onKey(163, 1)
        keys.clear()
        assertEquals(CarPlayMediaButton.NEXT, keys.onKey(163, 1))
    }
}
