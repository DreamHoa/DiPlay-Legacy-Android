package com.shilapi.xcertplay.transport

import org.junit.Assert.*
import org.junit.Test

class LegacyUsbReadDeadlineTest {
    @Test fun aNormalBulkTimeoutReturnsWithoutAnotherTransfer() {
        var now = 0L
        var calls = 0
        val count = readUsbWithDeadline(1000, {}, {
            calls++
            assertEquals(1000, it)
            now = 1_000_000_000L
            -1
        }, { now }, { fail("An elapsed timeout must not retry") })
        assertNull(count)
        assertEquals(1, calls)
    }

    @Test fun immediateFailuresArePacedAndStillEndAtTheDeadline() {
        var now = 0L
        var calls = 0
        val count = readUsbWithDeadline(25, {}, { calls++; -1 }, { now }, {
            assertTrue(it in 1..10)
            now += it * 1_000_000L
        })
        assertNull(count)
        assertEquals(3, calls)
        assertEquals(25_000_000L, now)
    }

    @Test fun closeIsObservedBeforeRetryingAFailedTransfer() {
        var closed = false
        var calls = 0
        try {
            readUsbWithDeadline(1000, {
                if (closed) throw IphoneUsbException.DeviceUnavailable("closed")
            }, { calls++; -1 }, { 0L }, { closed = true })
            fail("The read must stop when the session closes")
        } catch (_: IphoneUsbException.DeviceUnavailable) {
            assertEquals(1, calls)
        }
    }

    @Test fun aSuccessfulReadIsReturnedWithoutRetrying() {
        assertEquals(7, readUsbWithDeadline(1000, {}, { 7 }, { 0L }, {
            fail("Successful reads do not pause")
        }))
    }
}
