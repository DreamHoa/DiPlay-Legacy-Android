package com.shilapi.xcertplay.transport

import com.shilapi.xcertplay.iap2.body.Iap2BodyReader
import com.shilapi.xcertplay.iap2.wire.Iap2ParameterList
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Iap2WiredControlClientTest {
    @Test fun firstAirPlayDeadlineExpiresEvenWhileControlMessagesKeepArriving() {
        val started = 1_000_000_000L
        for (elapsedSeconds in 0..30) {
            val timeout = Iap2WiredControlClient.firstAirPlayPollTimeoutMillis(
                started, 300_000, false, started + elapsedSeconds * 1_000_000_000L,
            )
            assertEquals((30 - elapsedSeconds) * 1000L, timeout)
        }
    }

    @Test fun firstAirPlayDeadlineDoesNotLimitAnEstablishedSession() {
        assertEquals(300_000L, Iap2WiredControlClient.firstAirPlayPollTimeoutMillis(
            1_000_000_000L, 300_000, true, 99_000_000_000L,
        ))
        assertEquals(300_000L, Iap2WiredControlClient.firstAirPlayPollTimeoutMillis(
            0L, 300_000, false, 99_000_000_000L,
        ))
    }

    @Test fun firstAirPlayPollRespectsOtherDeadlinesAndNeverUsesInfiniteUsbTimeoutZero() {
        assertEquals(100L, Iap2WiredControlClient.firstAirPlayPollTimeoutMillis(
            1_000_000_000L, 100, false, 2_000_000_000L,
        ))
        assertEquals(1L, Iap2WiredControlClient.firstAirPlayPollTimeoutMillis(
            1_000_000_000L, 100, false, 30_999_999_999L,
        ))
    }

    @Test
    fun carPlayStartSessionNestsAddressesInsideWiredAttributes() {
        val frame = Iap2WiredControlClient.carPlayStartSession(
            Iap2WiredCarPlayEndpoint(
                ipv6Addresses = listOf("fe80::2"),
                airPlayPort = 7000,
                deviceIdentifier = "02:00:00:00:00:02",
                publicKey = "abcd",
                sourceVersion = "1.0",
            ),
        )

        assertEquals(0x4301, frame.messageId)
        val outer = Iap2BodyReader.of(frame).list()
        val wired = outer.single { it.id == 0 }
        val addresses = Iap2ParameterList.parse(wired.payload).asList()
        assertEquals(1, addresses.size)
        assertEquals(0, addresses.single().id)
        assertArrayEquals("fe80::2\u0000".encodeToByteArray(), addresses.single().payload)
    }
}
