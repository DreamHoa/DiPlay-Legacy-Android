package com.shilapi.xcertplay.navigation

import com.shilapi.xcertplay.iap2.wire.Iap2Frame

/** Phone call state used to avoid consuming wheel keys during calls. No vehicle IPC. */
object CarPlayCallMonitor {
    private val calls = CarPlayCallState()
    fun onFrame(frame: Iap2Frame) = synchronized(calls) { calls.accept(frame) }
    fun current(): CarPlayCallCard? = synchronized(calls) { calls.current() }
    fun clear() = synchronized(calls) { calls.clear() }
}
