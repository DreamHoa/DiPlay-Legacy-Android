package com.shilapi.xcertplay

import com.shilapi.xcertplay.airplay.*
import com.shilapi.xcertplay.orchestration.*
import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
// Robolectric 4.17 supports API 23 and newer; API 22 still requires device validation.
@Config(sdk = [23, 28], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class WiredSessionStartupRegressionTest {
    private class Fixture : AutoCloseable {
        private val config = AirPlayConfig("test", "test", "", "1", AirPlayDisplayConfig(800, 480))
        private val identity = AirPlayIdentity(ByteArray(32), ByteArray(32), "test")
        val controller = CarPlayController(RuntimeEnvironment.getApplication(),
            CarPlayRuntimeConfig(mfiTarget = MfiTarget.LOCAL, transport = CarPlayTransport.WIRED,
                identification = Iap2IdentificationConfig(name = "test", modelIdentifier = "test",
                    manufacturer = "test", serialNumber = "test", firmwareVersion = "1",
                    hardwareVersion = "1", carPlayUsbInterfaceNumber = 3)),
            config, identity, PairingStore(), object : AirPlaySessionListener {},
            object : AirPlayMediaHandler {}, {})
        private val phone = AirPlaySession(Socket(), config, identity, PairingStore(), null,
            object : AirPlaySessionListener {}, object : AirPlayMediaHandler {})
        private val listener = controller.javaClass.getDeclaredField("sessionListener")
            .apply { isAccessible = true }.get(controller) as AirPlaySessionListener

        fun record() = listener.onSessionActive(phone)
        fun progress(message: String = "iap2 tx=0x4301 carplay-start-session") {
            controller.javaClass.getDeclaredMethod("onWiredControlProgress", String::class.java)
                .apply { isAccessible = true }.invoke(controller, message)
        }
        fun connectionProven(): Boolean = (controller.javaClass.getDeclaredField("wiredTcpArrived")
            .apply { isAccessible = true }.get(controller) as AtomicBoolean).get()

        override fun close() {
            controller.close()
            assertTrue(controller.awaitClosed(2_000))
            phone.close()
        }
    }

    @Test fun startupMessagesWithoutRecordDoNotPretendThePhoneConnected() {
        Fixture().use { f ->
            repeat(3) { f.progress() }
            assertFalse(f.connectionProven())
        }
    }

    @Test fun recordBeforeTheStartupLogIsNotOverwritten() {
        Fixture().use { f ->
            f.record()
            f.progress()
            assertTrue("The phone can finish RECORD before the StartSession log callback", f.connectionProven())
        }
    }

    @Test fun duplicateAvailabilityRepliesPreserveAnEstablishedSessionWithoutAudioOrVideoFrames() {
        Fixture().use { f ->
            f.progress()
            f.record()
            repeat(3) {
                f.progress()
                assertTrue("A repeated startup reply must not re-arm first-session failure", f.connectionProven())
            }
        }
    }

    @Test fun timeoutDiagnosticContainingStartSessionDoesNotInvalidateAConnection() {
        Fixture().use { f ->
            f.record()
            f.progress("iap2 wired no AirPlay TCP 30000ms after carplay-start-session")
            assertTrue(f.connectionProven())
        }
    }
}
