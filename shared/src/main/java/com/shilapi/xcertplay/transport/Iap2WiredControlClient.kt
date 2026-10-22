package com.shilapi.xcertplay.transport

import com.shilapi.xcertplay.iap2.message.Iap2CarPlayMessages
import com.shilapi.xcertplay.iap2.message.Iap2ControlMessages
import com.shilapi.xcertplay.iap2.session.Iap2Session
import com.shilapi.xcertplay.iap2.wire.Iap2Frame
import com.shilapi.xcertplay.mfi.Iap2MfiAuthenticationClient
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress

/**
 * The wired LIVI control sequence after a CSM channel is ready:
 * Identification, MFi, power announcement, five update subscriptions, then CarPlay availability.
 *
 * The caller retains ownership of [session]. While [run] is active it is the only receiver and
 * forwards each non-availability CSM frame to [onIncoming]; it neither opens NCM nor implements
 * an AirPlay receiver.
 */
class Iap2WiredControlClient(
    private val session: Iap2Session,
    private val mfi: Iap2MfiAuthenticationClient,
    private val peerIdleDiagnosticMillis: Long = PEER_IDLE_DIAGNOSTIC_MILLIS,
) {
    fun run(
        identification: Iap2IdentificationConfig,
        endpoint: Iap2WiredCarPlayEndpoint,
        availableCurrentMilliAmps: Int,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        locationProvider: Iap2LocationProvider? = null,
        vehicleStatusProvider: VehicleStatusProvider? = null,
        onIncoming: (Iap2Frame) -> Unit = {},
        onProgress: (String) -> Unit = {},
        /**
         * Issue #95: the iPhone may stay alive on iAP2 after StartSession yet never open the
         * AirPlay TCP connection. When supplied, the live loop fails fast with NO_AIRPLAY_TCP
         * instead of idling until the USB stack wedges. Defaults to disabled.
         */
        airPlayTcpArrived: () -> Boolean = { true },
        startSessionAgain: () -> StartAgain = { StartAgain.DONE },
    ): Iap2WiredControlResult {
        require(availableCurrentMilliAmps in 0..0xffff) {
            "availableCurrentMilliAmps must be in 0..65535"
        }
        require(timeoutMillis == NO_TIMEOUT_MILLIS || timeoutMillis in 1..MAX_TIMEOUT_MILLIS) {
            "timeoutMillis must be in 1..$MAX_TIMEOUT_MILLIS or NO_TIMEOUT_MILLIS"
        }

        val deadlineNanos = Iap2ControlDeadline(timeoutMillis)
        val identified = identification.withVehicleStatusFrom(vehicleStatusProvider)
        if (identified.vehicleStatusEnabled != identification.vehicleStatusEnabled) {
            onProgress("iap2 no battery reading: not declaring an electric vehicle")
        }
        Iap2IdentificationClient(session).identify(identified, requireRemaining(deadlineNanos))
        onProgress("iap2 identification accepted")
        var stage = Iap2WiredControlStage.IDENTIFIED
        mfi.run(session, requireRemaining(deadlineNanos), onProgress)
        stage = Iap2WiredControlStage.AUTHENTICATED
        onProgress("iap2 authentication accepted")
        deadlineNanos.authenticated()

        send(powerSourceUpdate(availableCurrentMilliAmps), deadlineNanos)
        for (subscription in subscriptions()) send(subscription, deadlineNanos)
        stage = Iap2WiredControlStage.SUBSCRIBED
        onProgress("iap2 power/subscriptions sent")

        var forwardedFrames = 0
        var carPlayStartSessions = 0
        var startSessionNanos = 0L
        var startAgainDecided = false
        // Quiet CSM traffic is diagnostic, not proof that the AirPlay session is dead.
        // The Okavango's working wired session can go minutes without another CSM frame.
        val silenceMonitor = Iap2PeerSilenceMonitor(peerIdleDiagnosticMillis)
        // Only the initial CarPlay availability handshake is time-bounded; live control stays open.
        val startupDeadlineNanos = System.nanoTime() + STARTUP_AVAILABILITY_TIMEOUT_MILLIS * NANOS_PER_MILLISECOND
        val location = Iap2LocationReporter(locationProvider, onProgress)
        val vehicleStatus = Iap2VehicleStatusReporter(vehicleStatusProvider, onProgress)
        try {
            while (true) {
                val remaining = remainingMillis(deadlineNanos)
                if (remaining == 0L) {
                    return Iap2WiredControlResult(Iap2WiredControlTerminal.TIMED_OUT, stage, forwardedFrames, carPlayStartSessions)
                }
                location.tick { send(it, deadlineNanos) }
                vehicleStatus.tick { send(it, deadlineNanos) }
                // Only retry a previously sent session request if USB came up late and the
                // iPhone has not attempted our AirPlay listener since the link opened.
                var startAgainPending = false
                if (!startAgainDecided && stage == Iap2WiredControlStage.CARPLAY_START_SENT) {
                    when (startSessionAgain()) {
                        StartAgain.WAIT -> startAgainPending = true
                        StartAgain.DONE -> startAgainDecided = true
                        StartAgain.SEND -> {
                            startAgainDecided = true
                            send(carPlayStartSession(endpoint), deadlineNanos)
                            carPlayStartSessions++
                            startSessionNanos = System.nanoTime()
                            onProgress(START_SESSION_AGAIN_PROGRESS)
                        }
                    }
                }
                val startupRemaining = if (carPlayStartSessions == 0) {
                    ((startupDeadlineNanos - System.nanoTime()) / NANOS_PER_MILLISECOND).coerceAtLeast(0L)
                } else remaining
                if (startupRemaining == 0L) {
                    onProgress("iap2 wired startup timed out waiting for CarPlay availability")
                    return Iap2WiredControlResult(Iap2WiredControlTerminal.TIMED_OUT, stage, forwardedFrames, carPlayStartSessions)
                }
                // Other iAP2 traffic does not prove the AirPlay data path is ready. Check this
                // on every loop and cap recv itself so an idle read cannot postpone recovery.
                val firstTcpPollTimeout = firstAirPlayPollTimeoutMillis(
                    startSessionNanos, minOf(remaining, startupRemaining), airPlayTcpArrived(),
                )
                if (firstTcpPollTimeout == 0L) {
                    onProgress("iap2 wired no AirPlay TCP ${WIRED_FIRST_TCP_MILLIS}ms after carplay-start-session")
                    return Iap2WiredControlResult(
                        Iap2WiredControlTerminal.NO_AIRPLAY_TCP, stage, forwardedFrames, carPlayStartSessions,
                    )
                }
                val pollTimeout = vehicleStatus.pollTimeout(location.pollTimeout(firstTcpPollTimeout)).let {
                    if (startAgainPending) minOf(it, START_AGAIN_POLL_MILLIS) else it
                }
                val incoming = try {
                    session.recv(pollTimeout)
                } catch (failure: IOException) {
                    if (session.isClosed) {
                        return Iap2WiredControlResult(
                            Iap2WiredControlTerminal.CHANNEL_CLOSED,
                            stage,
                            forwardedFrames,
                            carPlayStartSessions,
                        )
                    }
                    throw failure
                }
                if (incoming == null) {
                    if (session.isClosed) {
                        return Iap2WiredControlResult(
                            Iap2WiredControlTerminal.CHANNEL_CLOSED,
                            stage,
                            forwardedFrames,
                            carPlayStartSessions,
                        )
                    }
                    if (remainingMillis(deadlineNanos) == 0L) {
                        return Iap2WiredControlResult(
                            Iap2WiredControlTerminal.TIMED_OUT,
                            stage,
                            forwardedFrames,
                            carPlayStartSessions,
                        )
                    }
                    // Keep reporting CSM inactivity without tearing down working video/audio.
                    // Startup deadlines, channel closure and transport errors still recover.
                    silenceMonitor.diagnosticIfDue()?.let { onProgress(it) }
                    continue
                }
                silenceMonitor.onFrameReceived()

                when (incoming.messageId) {
                    CARPLAY_AVAILABILITY -> {
                        onProgress("iap2 rx=0x4300 carplay-availability")
                        onProgress(carPlayAvailabilitySummary(incoming.payload))
                        // LIVI sends its wired answer on every availability notification; do not gate it on
                        // the phone's advertised availability boolean.
                        send(carPlayStartSession(endpoint), deadlineNanos)
                        stage = Iap2WiredControlStage.CARPLAY_START_SENT
                        carPlayStartSessions++
                        if (startSessionNanos == 0L) startSessionNanos = System.nanoTime()
                        onProgress("iap2 tx=0x4301 carplay-start-session")
                    }

                    Iap2LocationMessages.START_LOCATION_INFORMATION, Iap2LocationMessages.STOP_LOCATION_INFORMATION -> {
                        location.handle(incoming) { send(it, deadlineNanos) }
                    }

                    Iap2VehicleStatus.START_VEHICLE_STATUS_UPDATES, Iap2VehicleStatus.STOP_VEHICLE_STATUS_UPDATES -> {
                        vehicleStatus.handle(incoming) { send(it, deadlineNanos) }
                    }

                    else -> {
                        onProgress("iap2 rx=0x${incoming.messageId.toString(16).padStart(4, '0')}")
                        onIncoming(incoming)
                        forwardedFrames++
                    }
                }
            }
        } finally {
            locationProvider?.stop()
        }
    }

    private fun send(frame: Iap2Frame, deadlineNanos: Iap2ControlDeadline) {
        session.send(frame, requireRemaining(deadlineNanos))
    }

    /** Late NCM readiness can require one second CarPlayStartSession request. */
    enum class StartAgain { WAIT, SEND, DONE }

    companion object {
        const val NO_TIMEOUT_MILLIS = Long.MAX_VALUE
        const val START_SESSION_AGAIN_PROGRESS =
            "iap2 tx=0x4301 carplay-start-session again: no AirPlay connection since USB link opened"
        private const val START_AGAIN_POLL_MILLIS = 500L
        private const val CARPLAY_AVAILABILITY = 0x4300
        private const val CARPLAY_START_SESSION = 0x4301
        private const val STARTUP_AVAILABILITY_TIMEOUT_MILLIS = 60_000L
        private const val DEFAULT_TIMEOUT_MILLIS = 60_000L
        private const val MAX_TIMEOUT_MILLIS = 24 * 60 * 60 * 1_000L
        private const val MAX_RECV_TIMEOUT_MILLIS = 5 * 60 * 1_000L
        private const val NANOS_PER_MILLISECOND = 1_000_000L
        /** Idle diagnostic cadence for a quiet iAP2 peer (issue #84/#61, Fix 0). */
        private const val PEER_IDLE_DIAGNOSTIC_MILLIS = 30_000L
        /** No AirPlay TCP this long after StartSession: fail fast (issue #95). Mirrors wireless. */
        private const val WIRED_FIRST_TCP_MILLIS = 30_000L

        /** Absolute first-TCP deadline: unrelated control frames never restart it. */
        internal fun firstAirPlayPollTimeoutMillis(
            startSessionNanos: Long,
            requestedMillis: Long,
            tcpArrived: Boolean,
            nowNanos: Long = System.nanoTime(),
        ): Long {
            if (startSessionNanos == 0L || tcpArrived) return requestedMillis
            val leftNanos = WIRED_FIRST_TCP_MILLIS * NANOS_PER_MILLISECOND - (nowNanos - startSessionNanos)
            if (leftNanos <= 0L) return 0L
            return minOf(requestedMillis, (leftNanos / NANOS_PER_MILLISECOND).coerceAtLeast(1L))
        }

        /** Exact LIVI wired PowerSourceUpdate encoding: current and the charge-if-powered flag. */
        fun powerSourceUpdate(availableCurrentMilliAmps: Int): Iap2Frame =
            Iap2ControlMessages.powerSourceUpdate(availableCurrentMilliAmps, charging = true)

        /** Exact five subscription requests emitted by LIVI's wired bring-up. */
        fun subscriptions(): List<Iap2Frame> = Iap2ControlMessages.subscriptions()

        /** Builds the wired-only CarPlayStartSession message; no NCM or AirPlay socket is opened. */
        fun carPlayStartSession(endpoint: Iap2WiredCarPlayEndpoint): Iap2Frame =
            Iap2CarPlayMessages.startSession(
                wiredIpv6Addresses = endpoint.ipv6Addresses,
                airPlayPort = endpoint.airPlayPort,
                deviceIdentifier = endpoint.deviceIdentifier,
                publicKey = endpoint.publicKey,
                sourceVersion = endpoint.sourceVersion,
            )

        fun carPlayAvailabilitySummary(payload: ByteArray): String {
            return try {
                val availability = Iap2CarPlayMessages.availability(payload).wired
                val available = availability?.available
                val transport = availability?.identifier
                "iap2 4300 wiredAvailable=$available usbTransport=${transport ?: "none"}"
            } catch (error: RuntimeException) {
                "iap2 4300 decode failed: ${error.message}"
            }
        }

        private fun requireRemaining(deadlineNanos: Iap2ControlDeadline): Long = remainingMillis(deadlineNanos).also {
            if (it == 0L) throw IphoneUsbException.TimedOut("Timed out during wired iAP2 control bring-up")
        }

        private fun remainingMillis(deadlineNanos: Iap2ControlDeadline): Long = deadlineNanos.remainingMillis()

    }
}

/** A configured wired AirPlay endpoint; configuration does not claim that either service is live. */
class Iap2WiredCarPlayEndpoint(
    ipv6Addresses: List<String>,
    val airPlayPort: Int,
    val publicKey: String,
    val sourceVersion: String,
    val deviceIdentifier: String? = null,
) {
    /** A stable copy so a caller cannot mutate a validated endpoint before it is encoded. */
    val ipv6Addresses: List<String> = ipv6Addresses.toList()

    init {
        require(ipv6Addresses.isNotEmpty()) { "At least one wired IPv6 address is required" }
        require(ipv6Addresses.all(::isIpv6Literal)) { "Every wired address must be an IPv6 text literal" }
        require(airPlayPort in 1..65535) { "airPlayPort must be in 1..65535" }
        require(publicKey.isNotEmpty()) { "publicKey is required and must not be empty" }
        require(sourceVersion.isNotEmpty()) { "sourceVersion is required and must not be empty" }
        require('\u0000' !in publicKey) { "publicKey must not contain U+0000" }
        require('\u0000' !in sourceVersion) { "sourceVersion must not contain U+0000" }
        deviceIdentifier?.let {
            require(it.isNotEmpty()) { "deviceIdentifier is optional, but must not be empty when provided" }
            require('\u0000' !in it) { "deviceIdentifier must not contain U+0000" }
        }
    }

    private companion object {
        fun isIpv6Literal(value: String): Boolean {
            if (value.contains('%') || '\u0000' in value || !value.contains(':')) return false
            return try {
                InetAddress.getByName(value) is Inet6Address
            } catch (_: Exception) {
                false
            }
        }
    }
}

enum class Iap2WiredControlStage {
    IDENTIFIED,
    AUTHENTICATED,
    SUBSCRIBED,
    CARPLAY_START_SENT,
}

enum class Iap2WiredControlTerminal { CHANNEL_CLOSED, TIMED_OUT, NO_AIRPLAY_TCP }

/** End state of the control loop only; it is not evidence of NCM or AirPlay availability. */
data class Iap2WiredControlResult(
    val terminal: Iap2WiredControlTerminal,
    val stage: Iap2WiredControlStage,
    val forwardedFrames: Int,
    val carPlayStartSessionsSent: Int,
)
