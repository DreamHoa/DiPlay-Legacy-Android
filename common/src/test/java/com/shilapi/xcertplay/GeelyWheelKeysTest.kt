package com.shilapi.xcertplay

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class GeelyWheelKeysTest {
    private val sent = mutableListOf<Pair<Int, Int>>()
    private val tasks = mutableListOf<Runnable>()
    private val keys = GeelyWheelKeys(worker = Executor { tasks += it }) { index, key -> sent += index to key }
    private val component = ComponentName(GeelyWheelKeys.PACKAGE, GeelyWheelKeys.SERVICE)
    private val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
        lateinit var connection: ServiceConnection
        var binds = 0
        var acceptBind = true

        override fun getApplicationContext(): Context = this

        override fun bindService(service: Intent, conn: ServiceConnection, flags: Int): Boolean {
            assertEquals(GeelyWheelKeys.ACTION, service.action)
            assertEquals(component, service.component)
            assertEquals(Context.BIND_AUTO_CREATE, flags)
            binds++
            connection = conn
            return acceptBind
        }
    }

    @Test fun usesAdditiveEcarxRegistrationAndBindsOnceForRepeatedPlayback() {
        val remote = connect()
        repeat(3) { keys.ensureBound(context); drain() }
        assertEquals(1, context.binds)
        assertEquals(1, remote.registrations)
        remote.key(163, 1)
        assertTrue(sent.isEmpty()) // Binder callback returns before the main-thread CarPlay work.
        idle()
        remote.key(163, 0)
        remote.key(165, 1)
        remote.key(165, 0)
        idle()
        assertEquals(listOf(CarPlayMediaButton.NEXT to 163, CarPlayMediaButton.PREVIOUS to 165), sent)
    }

    @Test fun droppedConnectionDiscardsQueuedKeysAndReconnectRegistersNewService() {
        val first = connect()
        first.key(163, 1)
        context.connection.onServiceDisconnected(component)
        idle()
        assertTrue(sent.isEmpty())
        val second = VendorService()
        context.connection.onServiceConnected(component, second)
        drain()
        second.key(163, 1)
        idle()
        assertEquals(listOf(CarPlayMediaButton.NEXT to 163), sent)
        assertEquals(1, second.registrations)
    }

    @Test fun failedBindingCanBeRetried() {
        context.acceptBind = false
        keys.ensureBound(context)
        context.acceptBind = true
        keys.ensureBound(context)
        val remote = VendorService()
        context.connection.onServiceConnected(component, remote)
        drain()
        assertEquals(2, context.binds)
        assertEquals(1, remote.registrations)
    }

    @Test fun failedRegistrationCanBeRetriedWithoutBindingOrReplacingOtherCallbacks() {
        val remote = VendorService().apply { acceptRegistration = false }
        keys.ensureBound(context)
        context.connection.onServiceConnected(component, remote)
        drain()
        remote.acceptRegistration = true
        keys.ensureBound(context)
        drain()
        remote.key(163, 1)
        idle()
        assertEquals(listOf(CarPlayMediaButton.NEXT to 163), sent)
        assertEquals(1, context.binds)
        assertEquals(2, remote.registrations)
    }

    @Test fun staleRegistrationWorkDoesNotTouchDisconnectedService() {
        keys.ensureBound(context)
        val first = VendorService()
        context.connection.onServiceConnected(component, first)
        context.connection.onServiceDisconnected(component)
        val second = VendorService()
        context.connection.onServiceConnected(component, second)
        drain()
        assertEquals(0, first.registrations)
        assertEquals(1, second.registrations)
    }

    private fun connect(): VendorService {
        val remote = VendorService()
        keys.ensureBound(context)
        context.connection.onServiceConnected(component, remote)
        drain()
        return remote
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun drain() {
        while (tasks.isNotEmpty()) tasks.removeAt(0).run()
        idle()
    }

    /** Emulates the original APK's wire contract, including its synchronous two-int callback. */
    private class VendorService : Binder() {
        var registrations = 0
        var acceptRegistration = true
        private lateinit var callback: IBinder

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            assertEquals(10, code) // Audio/radio/Bluetooth single-slot setters must never be called.
            data.enforceInterface("com.neusoft.optimus.wheeljack.popview.AIDL.IRequestInterface")
            registrations++
            if (!acceptRegistration) return false
            callback = requireNotNull(data.readStrongBinder())
            reply?.writeNoException()
            return true
        }

        fun key(key: Int, action: Int) {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken("com.neusoft.optimus.wheeljack.popview.AIDL.IEcarxkeyInterface")
                data.writeInt(key)
                data.writeInt(action)
                assertTrue(callback.transact(1, data, reply, 0))
                reply.readException()
            } finally {
                reply.recycle()
                data.recycle()
            }
        }
    }
}
