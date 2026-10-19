package com.shilapi.xcertplay.compat

import android.os.Build

/**
 * Returns the API level, retaining the Android 5.1 vendor release check for firmware that fakes
 * [Build.VERSION.SDK_INT]. Pre-Android-5.1 compatibility is not included.
 */
internal fun realSdkInt(): Int {
    val reported = Build.VERSION.SDK_INT
    val release = Build.VERSION.RELEASE ?: return reported
    if (release.startsWith("5.1") && reported > Build.VERSION_CODES.LOLLIPOP_MR1) {
        return Build.VERSION_CODES.LOLLIPOP_MR1
    }
    // Map 6.0 release to API 23.
    if (release.startsWith("6.0") && reported > Build.VERSION_CODES.M) {
        return Build.VERSION_CODES.M
    }
    return reported
}
