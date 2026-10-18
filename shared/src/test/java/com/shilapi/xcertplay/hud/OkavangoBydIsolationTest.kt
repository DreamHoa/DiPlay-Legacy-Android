package com.shilapi.xcertplay.hud

import org.junit.Assert.*
import org.junit.Test

class OkavangoBydIsolationTest {
    @Test fun factoryE01NeverStartsBydHardwareServices() {
        assertTrue(BydNavigationOutputs.isOkavango(22, "E01", "mt6735", "VF12LQJ"))
        assertFalse(BydNavigationOutputs.isOkavango(21, "E01", "mt6735", "VF12LQJ"))
        assertFalse(BydNavigationOutputs.isOkavango(22, "FS11GQJ", "mt6735", "FS11GQJ"))
        assertFalse(BydNavigationOutputs.isOkavango(22, "E01", "mt6735", "other"))
    }
}
