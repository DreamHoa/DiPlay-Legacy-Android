package com.shilapi.xcertplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * The Philippine VF12LQJ sends wheel keys through PopView RPC/AIDL, without an Android KeyEvent.
 * The descriptors and transaction numbers below were checked against this car's original APK.
 * Only the additive Ecarx listener is used; the audio/radio/Bluetooth callbacks are single slots.
 *
 * Keep one subscription for the process: this firmware's unregister wraps the binder in a new proxy
 * and removes it from a list by object identity, so unbind/rebind on each song/session leaks listeners.
 * The caller gates delivery on the current CarPlay session, focus and call state instead.
 */
internal class GeelyWheelKeys(
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val worker: Executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "diplay-geely-wheel").apply { isDaemon = true }
    },
    private val send: (index: Int, vendorKey: Int) -> Unit,
) {
    private val presses = GeelyWheelPresses()
    private var binding = false
    @Volatile private var currentService: IBinder? = null
    @Volatile private var registeredService: IBinder? = null
    private var pendingService: IBinder? = null

    private val callback = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == IBinder.INTERFACE_TRANSACTION) {
                reply?.writeString(CALLBACK_DESCRIPTOR)
                return true
            }
            if (code != 1) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(CALLBACK_DESCRIPTOR)
            val key = data.readInt()
            val action = data.readInt()
            val source = currentService
            // A synchronous vendor callback must return promptly, without waiting for CarPlay/network.
            handler.post {
                if (source != null && currentService === source) {
                    presses.onKey(key, action)?.let { send(it, key) }
                }
            }
            reply?.writeNoException()
            return true
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            currentService = service
            presses.clear()
            register(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            currentService = null
            presses.clear()
            Log.w(TAG, "PopView disconnected; waiting for Android to reconnect")
        }
    }

    /** Called on the main thread, only after OkavangoProfile matches the observed firmware family. */
    fun ensureBound(context: Context) {
        if (binding) {
            currentService?.let(::register)
            return
        }
        val intent = Intent(ACTION).setComponent(ComponentName(PACKAGE, SERVICE))
        binding = runCatching {
            context.applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }.onFailure { Log.w(TAG, "PopView wheel binding unavailable", it) }.getOrDefault(false)
        Log.i(TAG, "PopView wheel binding requested=$binding")
    }

    private fun register(service: IBinder) {
        if (registeredService === service || pendingService === service) return
        pendingService = service
        worker.execute {
            var registered = false
            try {
                if (currentService === service) {
                    val data = Parcel.obtain()
                    val reply = Parcel.obtain()
                    try {
                        data.writeInterfaceToken(REQUEST_DESCRIPTOR)
                        data.writeStrongBinder(callback)
                        check(service.transact(REGISTER, data, reply, 0)) { "Ecarx registration unsupported" }
                        reply.readException()
                        registeredService = service
                        registered = true
                    } finally {
                        reply.recycle()
                        data.recycle()
                    }
                }
            } catch (error: Exception) {
                Log.w(TAG, "PopView wheel registration failed", error)
            }
            handler.post {
                if (pendingService === service) pendingService = null
                if (registered && currentService === service) Log.i(TAG, "PopView Ecarx wheel listener registered")
            }
        }
    }

    companion object {
        private const val TAG = "DiPlay-GeelyWheel"
        internal const val PACKAGE = "com.neusoft.optimus.wheeljack.popview"
        internal const val SERVICE = "$PACKAGE.filters.AppAidlService"
        internal const val ACTION = "com.neusoft.HardKeyAidlInterface"
        internal const val REQUEST_DESCRIPTOR = "$PACKAGE.AIDL.IRequestInterface"
        internal const val CALLBACK_DESCRIPTOR = "$PACKAGE.AIDL.IEcarxkeyInterface"
        internal const val REGISTER = 10
    }
}

/** PopView uses Linux-style numbers and 1=pressed/0=released/2=held, not Android KeyEvent actions. */
internal class GeelyWheelPresses {
    private val pressed = mutableSetOf<Int>()

    fun onKey(key: Int, action: Int): Int? {
        val index = when (key) {
            163 -> CarPlayMediaButton.NEXT
            165 -> CarPlayMediaButton.PREVIOUS
            else -> return null
        }
        return when (action) {
            1 -> index.takeIf { pressed.add(key) }
            0 -> { pressed.remove(key); null }
            2 -> { pressed.add(key); null }
            else -> null
        }
    }

    fun clear() = pressed.clear()
}
