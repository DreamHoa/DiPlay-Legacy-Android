package com.shilapi.xcertplay.orchestration

import com.shilapi.xcertplay.iap2.body.Iap2BodyReader
import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import com.shilapi.xcertplay.transport.Iap2WiredControlClient
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WiredPowerBudgetTest {
    @Test fun defaultConfigurationAnnouncesFiveHundredMilliampsOnTheWire() {
        val config = configuration()
        val frame = Iap2WiredControlClient.powerSourceUpdate(config.availableCurrentMilliAmps)
        val parameters = Iap2BodyReader.of(frame).list()

        assertEquals(0xae03, frame.messageId)
        assertArrayEquals(byteArrayOf(0x01, 0xf4.toByte()), parameters.single { it.id == 0 }.payload)
        assertArrayEquals(byteArrayOf(1), parameters.single { it.id == 1 }.payload)
    }

    @Test fun verifiedDeploymentBudgetIsNotSilentlyReplacedByTheDefault() {
        val config = configuration(1000)
        val frame = Iap2WiredControlClient.powerSourceUpdate(config.availableCurrentMilliAmps)
        val parameters = Iap2BodyReader.of(frame).list()

        assertArrayEquals(byteArrayOf(0x03, 0xe8.toByte()), parameters.single { it.id == 0 }.payload)
    }

    private fun configuration(current: Int? = null): CarPlayRuntimeConfig {
        val identity = Iap2IdentificationConfig(
            name = "test", modelIdentifier = "test", manufacturer = "test",
            serialNumber = "test", firmwareVersion = "1", hardwareVersion = "1",
            carPlayUsbInterfaceNumber = 3,
        )
        return if (current == null) {
            CarPlayRuntimeConfig(mfiTarget = MfiTarget.LOCAL, identification = identity)
        } else {
            CarPlayRuntimeConfig(mfiTarget = MfiTarget.LOCAL, identification = identity,
                availableCurrentMilliAmps = current)
        }
    }
}
