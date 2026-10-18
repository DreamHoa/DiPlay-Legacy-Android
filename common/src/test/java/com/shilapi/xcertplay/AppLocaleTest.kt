package com.shilapi.xcertplay

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class AppLocaleTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val manager get() = context.getSystemService(LocaleManager::class.java)

    @Test fun onlyEnglishAndSimplifiedChineseMayBeSelected() {
        assertEquals(listOf(AppLocale.SYSTEM, AppLocale.ENGLISH, AppLocale.SIMPLIFIED_CHINESE), AppLocale.ALL)
        AppLocale.save(context, AppLocale.ENGLISH)
        assertEquals("en", manager.applicationLocales.toLanguageTags())
        AppLocale.save(context, AppLocale.SIMPLIFIED_CHINESE)
        assertEquals("zh-CN", manager.applicationLocales.toLanguageTags())
        AppLocale.save(context, AppLocale.SYSTEM)
        assertTrue(manager.applicationLocales.isEmpty)
        assertEquals(AppLocale.SYSTEM, AppLocale.preference(context))
    }

    @Test fun unsupportedPlatformLanguageFallsBackToSystem() {
        manager.applicationLocales = LocaleList.forLanguageTags("es")
        assertEquals(AppLocale.SYSTEM, AppLocale.preference(context))
        for (tag in listOf("zh-CN", "zh-TW", "zh-Hant-HK")) {
            manager.applicationLocales = LocaleList.forLanguageTags(tag)
            assertEquals(AppLocale.SIMPLIFIED_CHINESE, AppLocale.preference(context))
        }
        manager.applicationLocales = LocaleList.forLanguageTags("en-US")
        assertEquals(AppLocale.ENGLISH, AppLocale.preference(context))
    }

    @Test fun unsupportedLegacyLanguageDoesNotMigrate() {
        context.getSharedPreferences("diplay", Context.MODE_PRIVATE).edit()
            .putString("app_language", "ar")
            .putBoolean("app_language_platform_migrated", false)
            .commit()
        manager.applicationLocales = LocaleList.getEmptyLocaleList()
        assertSame(context, AppLocale.wrap(context))
        assertTrue(manager.applicationLocales.isEmpty)
        assertEquals(AppLocale.SYSTEM, AppLocale.preference(context))
    }

    @Test @Config(sdk = [28, 32])
    fun olderAndroidUsesEnglishOrSimplifiedChineseOnly() {
        AppLocale.save(context, AppLocale.ENGLISH)
        assertEquals("en", AppLocale.wrap(context).resources.configuration.locales[0].language)
        AppLocale.save(context, AppLocale.SIMPLIFIED_CHINESE)
        assertEquals("zh", AppLocale.wrap(context).resources.configuration.locales[0].language)
        context.getSharedPreferences("diplay", Context.MODE_PRIVATE).edit()
            .putString("app_language", "ru").commit()
        assertEquals(AppLocale.SYSTEM, AppLocale.preference(context))
        assertSame(context, AppLocale.wrap(context))
    }
}
