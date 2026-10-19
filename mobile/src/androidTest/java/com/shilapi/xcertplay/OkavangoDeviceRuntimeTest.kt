package com.shilapi.xcertplay

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import com.shilapi.xcertplay.airplay.*
import com.shilapi.xcertplay.media.AndroidMediaSink
import android.content.Context
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real E01 runtime checks; no Bluetooth switch, USB session or vendor maintenance. */
class OkavangoDeviceRuntimeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    @Test fun deviceAndSavedDefaultsMatchObservedE01() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(22, Build.VERSION.SDK_INT)
        assertTrue(OkavangoProfile.matches(Build.VERSION.SDK_INT, Build.MODEL, Build.HARDWARE, Build.DEVICE))
        assertEquals("com.shihab.diplay.okavango", context.packageName)
        assertEquals(30, AirPlayPersistence.loadFps(context))
        assertEquals(67, AirPlayPersistence.loadDisplayScalePercent(context))
        assertFalse(AirPlayPersistence.loadHevcEnabled(context))
    }

    @Test fun pairingCryptoWorksOnE01Runtime() {
        val first = AirPlayCrypto.x25519Generate()
        val second = AirPlayCrypto.x25519Generate()
        assertArrayEquals(AirPlayCrypto.x25519Shared(first.privateKey, second.publicKey),
            AirPlayCrypto.x25519Shared(second.privateKey, first.publicKey))
        val signing = AirPlayCrypto.ed25519Generate()
        val payload = byteArrayOf(1, 2, 3, 4)
        assertTrue(AirPlayCrypto.ed25519Verify(signing.publicKey, payload,
            AirPlayCrypto.ed25519Sign(signing.privateKey, payload)))
        val key = AirPlayCrypto.hkdfSha512(first.privateKey, byteArrayOf(5), byteArrayOf(6))
        val nonce = AirPlayCrypto.nonce64(1)
        val sealed = AirPlayCrypto.chachaSeal(key, nonce, payload)
        assertArrayEquals(payload, AirPlayCrypto.chachaOpen(key, nonce, sealed))
    }

    @Test fun selectedLocalAuthenticationLoadsAndSignsOnE01() {
        assumeTrue("Source-only APK has no accessory identity",
            context.assets.list("offline-mfi")?.contains("identity.pk8") == true)
        val bootstrap = Class.forName("com.shilapi.xcertplay.DiPlayBootstrap")
        bootstrap.getMethod("ensure", Context::class.java, com.shilapi.xcertplay.orchestration.MfiTarget::class.java)
            .invoke(bootstrap.getField("INSTANCE").get(null), context, com.shilapi.xcertplay.orchestration.MfiTarget.LOCAL)
        val root = context.noBackupFilesDir
        val client = com.shilapi.xcertplay.mfi.LocalMfiAuthenticationClient.load(java.io.File(root, "offline-mfi"))
        assertTrue(client.readCertificate(16384).isNotEmpty())
        assertEquals(64, client.signChallenge(ByteArray(32)).size)
    }

    @Test fun h264FrameActuallyDecodesOnE01() {
        val texture = android.graphics.SurfaceTexture(1)
        val surface = android.view.Surface(texture)
        val rendered = CountDownLatch(1)
        val frameAvailable = CountDownLatch(1)
        texture.setOnFrameAvailableListener { frameAvailable.countDown() }
        val sink = AndroidMediaSink(surface = surface, videoWidth = 320, videoHeight = 240)
        val lines = Collections.synchronizedList(mutableListOf<String>())
        sink.setVideoDiagnosticHandler(0) { line ->
            lines += line
            if (line.contains("first frame rendered")) rendered.countDown()
        }
        val encoder = android.media.MediaCodec.createEncoderByType("video/avc")
        var encoderStarted = false
        try {
            val format = android.media.MediaFormat.createVideoFormat("video/avc", 320, 240).apply {
                setInteger(android.media.MediaFormat.KEY_COLOR_FORMAT,
                    android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
                setInteger(android.media.MediaFormat.KEY_BIT_RATE, 500000)
                setInteger(android.media.MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            encoder.configure(format, null, null, android.media.MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            encoderStarted = true
            val info = android.media.MediaCodec.BufferInfo()
            var submitted = 0
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (System.nanoTime() < deadline && rendered.count > 0) {
                if (submitted < 10) {
                    val input = encoder.dequeueInputBuffer(10000)
                    if (input >= 0) {
                        val pixels = ByteArray(320 * 240 * 3 / 2) { 128.toByte() }
                        encoder.getInputBuffer(input)!!.apply { clear(); put(pixels) }
                        encoder.queueInputBuffer(input, 0, pixels.size, submitted * 33333L, 0)
                        submitted++
                    }
                }
                val output = encoder.dequeueOutputBuffer(info, 10000)
                if (output == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    fun parameter(name: String): ByteArray {
                        val buffer = encoder.outputFormat.getByteBuffer(name)!!.duplicate()
                        val bytes = ByteArray(buffer.remaining()).also(buffer::get)
                        val prefix = if (bytes[2] == 1.toByte()) 3 else 4
                        return bytes.copyOfRange(prefix, bytes.size)
                    }
                    val sps = parameter("csd-0")
                    val pps = parameter("csd-1")
                    val avc = byteArrayOf(1, sps[1], sps[2], sps[3], 0xff.toByte(), 0xe1.toByte(),
                        (sps.size ushr 8).toByte(), sps.size.toByte()) + sps +
                        byteArrayOf(1, (pps.size ushr 8).toByte(), pps.size.toByte()) + pps
                    sink.onVideoCodec(0, VideoCodec.H264)
                    sink.onVideoConfig(0, avc)
                } else if (output >= 0) {
                    if (info.flags and android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                        val buffer = encoder.getOutputBuffer(output)!!.duplicate().apply {
                            position(info.offset); limit(info.offset + info.size)
                        }
                        sink.onVideoFrame(0, carPlayAccessUnit(ByteArray(info.size).also(buffer::get)))
                    }
                    encoder.releaseOutputBuffer(output, false)
                }
            }
            assertTrue("First decoded frame: $lines", rendered.await(2, TimeUnit.SECONDS))
            assertTrue("Surface received no frame: $lines", frameAvailable.await(2, TimeUnit.SECONDS))
        } finally {
            if (encoderStarted) encoder.stop()
            encoder.release()
            sink.close()
            val released = sink.awaitVideoReleased(3000)
            surface.release()
            texture.release()
            assertTrue("Video decoder did not release", released)
        }
    }

    private fun carPlayAccessUnit(annexB: ByteArray): ByteArray {
        val units = mutableListOf<Pair<Int, Int>>()
        var cursor = 0
        while (cursor + 3 < annexB.size) {
            val prefix = when {
                annexB[cursor] != 0.toByte() || annexB[cursor + 1] != 0.toByte() -> 0
                annexB[cursor + 2] == 1.toByte() -> 3
                annexB[cursor + 2] == 0.toByte() && annexB[cursor + 3] == 1.toByte() -> 4
                else -> 0
            }
            if (prefix > 0) {
                units += cursor to cursor + prefix
                cursor += prefix
            } else cursor++
        }
        val output = java.io.ByteArrayOutputStream()
        val data = java.io.DataOutputStream(output)
        if (units.isEmpty()) {
            // The software AVC encoder can return a single raw slice NAL.
            check(annexB.isNotEmpty() && (annexB[0].toInt() and 31) in listOf(1, 5)) {
                "Unexpected encoder access unit, bytes=${annexB.size}, header=${annexB.firstOrNull()}"
            }
            data.writeInt(annexB.size)
            data.write(annexB)
            return output.toByteArray()
        }
        units.forEachIndexed { index, (_, start) ->
            val end = units.getOrNull(index + 1)?.first ?: annexB.size
            check(end > start)
            data.writeInt(end - start)
            data.write(annexB, start, end - start)
        }
        return output.toByteArray()
    }
}
