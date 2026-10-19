package com.shilapi.xcertplay

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.shilapi.xcertplay.host.R
import java.io.ByteArrayOutputStream

/** Fixed wordless Geely shield for the Okavango's CarPlay return-to-car icon. */
internal object GeelyCarIcon {
    fun loadBitmap(context: Context): Bitmap? = ContextCompat.getDrawable(
        context, R.drawable.ic_geely_auto_2023,
    )?.toBitmap(width = 512, height = 512, config = Bitmap.Config.ARGB_8888)

    fun loadBytes(context: Context): ByteArray? {
        val bitmap = loadBitmap(context) ?: return null
        return try {
            ByteArrayOutputStream().use { out ->
                if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) out.toByteArray() else null
            }
        } finally {
            bitmap.recycle()
        }
    }
}
