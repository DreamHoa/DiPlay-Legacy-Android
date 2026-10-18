// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.shilapi.xcertplay.host.R
import java.util.Locale

/** App-language override for the Okavango's Android 5.1 / API 22. */
object AppLocale {
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val SIMPLIFIED_CHINESE = "zh"
    val ALL = listOf(SYSTEM, ENGLISH, SIMPLIFIED_CHINESE)

    private const val PREFS = "diplay"
    private const val KEY_LANGUAGE = "app_language"

    fun preference(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SYSTEM)?.takeIf { it in ALL } ?: SYSTEM

    fun save(context: Context, language: String) {
        require(language in ALL)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LANGUAGE, language).apply()
    }

    fun wrap(context: Context): Context {
        val locale = locale(preference(context)) ?: return context
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    /** Restore an explicit language if an Android 5.1 head unit resets Activity resources. */
    @Suppress("DEPRECATION")
    fun enforce(context: Context): Boolean {
        val locale = locale(preference(context)) ?: Resources.getSystem().configuration.locale
        val resources = context.resources
        if (resources.configuration.locale == locale) return false
        resources.updateConfiguration(Configuration(resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }, resources.displayMetrics)
        return true
    }

    fun showPicker(activity: Activity) {
        var selected = ALL.indexOf(preference(activity)).coerceAtLeast(0)
        AlertDialog.Builder(activity)
            .setTitle(R.string.language_app_language)
            .setSingleChoiceItems(ALL.map { displayName(activity, it) }.toTypedArray(), selected) { _, index ->
                selected = index
            }
            .setPositiveButton(R.string.language_apply) { _, _ ->
                val next = ALL[selected]
                if (next != preference(activity)) {
                    save(activity, next)
                    activity.recreate()
                }
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    fun displayName(context: Context, language: String): String = when (language) {
        SYSTEM -> context.getString(R.string.language_system_default)
        ENGLISH -> "English"
        SIMPLIFIED_CHINESE -> "简体中文"
        else -> language
    }

    private fun locale(language: String): Locale? = when (language) {
        ENGLISH -> Locale.ENGLISH
        SIMPLIFIED_CHINESE -> Locale.SIMPLIFIED_CHINESE
        else -> null
    }
}
