package com.shilapi.xcertplay

import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class AppLocaleTest {
    private val app get() = RuntimeEnvironment.getApplication()

    @Test fun languagePickerOffersOnlySystemEnglishAndSimplifiedChinese() {
        assertEquals(listOf(AppLocale.SYSTEM, AppLocale.ENGLISH, AppLocale.SIMPLIFIED_CHINESE), AppLocale.ALL)
        assertEquals("English", AppLocale.displayName(app, AppLocale.ENGLISH))
        assertEquals("简体中文", AppLocale.displayName(app, AppLocale.SIMPLIFIED_CHINESE))
    }

    @Test fun explicitEnglishAndChineseWrapWithoutMutatingGlobalResources() {
        val original = app.resources.configuration.locales.toLanguageTags()
        try {
            AppLocale.save(app, AppLocale.ENGLISH)
            assertEquals("en", AppLocale.wrap(app).resources.configuration.locales[0].language)
            AppLocale.save(app, AppLocale.SIMPLIFIED_CHINESE)
            assertEquals("zh-CN", AppLocale.wrap(app).resources.configuration.locales.toLanguageTags())
            assertEquals(original, app.resources.configuration.locales.toLanguageTags())
        } finally {
            AppLocale.save(app, AppLocale.SYSTEM)
        }
    }

    @Test fun legacyUnsupportedLanguageFallsBackToSystem() {
        app.getSharedPreferences("diplay", Context.MODE_PRIVATE).edit()
            .putString("app_language", "ar").commit()
        assertEquals(AppLocale.SYSTEM, AppLocale.preference(app))
        assertSame(app, AppLocale.wrap(app))
        AppLocale.save(app, AppLocale.SYSTEM)
    }

    @Test fun enforcedChineseSurvivesHeadUnitLocaleReset() {
        try {
            AppLocale.save(app, AppLocale.SIMPLIFIED_CHINESE)
            @Suppress("DEPRECATION")
            app.resources.updateConfiguration(
                android.content.res.Configuration(app.resources.configuration).apply {
                    setLocale(java.util.Locale.ENGLISH)
                },
                app.resources.displayMetrics,
            )
            assertTrue(AppLocale.enforce(app))
            assertEquals("zh", app.resources.configuration.locales[0].language)
            assertFalse(AppLocale.enforce(app))
        } finally {
            AppLocale.save(app, AppLocale.SYSTEM)
            AppLocale.enforce(app)
        }
    }
}
