package com.shilapi.xcertplay.transport

/**
 * Reports CSM application inactivity on a live wired control loop.
 *
 * A working AirPlay session does not require periodic CSM messages. Inactivity alone
 * must not disconnect it; real channel closure, transport errors and startup deadlines
 * are handled by the caller. The injectable clock keeps diagnostics tests deterministic.
 */
internal class Iap2PeerSilenceMonitor(
    private val diagnosticMillis: Long,
    private val nowNanos: () -> Long = System::nanoTime,
) {
    private var lastRxNanos: Long = nowNanos()
    private var lastReportNanos: Long = 0L

    fun onFrameReceived() {
        lastRxNanos = nowNanos()
    }

    fun idleMillis(): Long = (nowNanos() - lastRxNanos) / 1_000_000L

    /**
     * A diagnostic line when the peer has been quiet for at least [diagnosticMillis]
     * and the previous report is old enough; null otherwise (rate-limited).
     */
    fun diagnosticIfDue(): String? {
        val idleMs = idleMillis()
        if (idleMs < diagnosticMillis) return null
        if (nowNanos() - lastReportNanos < diagnosticMillis * 1_000_000L) return null
        lastReportNanos = nowNanos()
        return "iap2 wired control idle idleMs=$idleMs (no iAP2 frame from iPhone)"
    }

}
