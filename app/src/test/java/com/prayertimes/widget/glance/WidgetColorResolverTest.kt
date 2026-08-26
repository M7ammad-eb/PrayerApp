package com.prayertimes.widget.glance

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.prayertimes.data.models.WidgetCustomizationSettings
import com.prayertimes.data.models.WidgetThemeMode
import com.prayertimes.data.preferences.AppPrayerSettings
import com.prayertimes.data.cities.CityDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetColorResolverTest {
    private fun resolve(
        showBackground: Boolean = true,
        opacityPercent: Int = 40,
        showBorder: Boolean = true
    ): WidgetColorScheme {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val settings = AppPrayerSettings(
            location = CityDatabase.defaultLocation(context.resources),
            widgetSettings = WidgetCustomizationSettings(
                themeMode = WidgetThemeMode.DARK_ELEGANT,
                showBackground = showBackground,
                opacityPercent = opacityPercent,
                showBorder = showBorder
            )
        )
        return WidgetColorResolver.resolve(context, settings)
    }

    @Test
    fun enabledBackgroundUsesSliderOpacityAndKeepsSupportingSurfacesAtZero() {
        val translucent = resolve(opacityPercent = 40)
        val zeroOpacity = resolve(opacityPercent = 0)

        assertEquals(102, Color.alpha(translucent.rootBgColor))
        assertEquals(0, Color.alpha(zeroOpacity.rootBgColor))
        assertTrue(Color.alpha(zeroOpacity.heroBgColor) > 0)
        assertTrue(Color.alpha(zeroOpacity.inactivePrayerBgColor) > 0)
    }

    @Test
    fun disabledBackgroundUsesTheCleanFloatingLayout() {
        val colors = resolve(showBackground = false, showBorder = false)

        assertEquals(0, Color.alpha(colors.rootBgColor))
        assertEquals(0, Color.alpha(colors.heroBgColor))
        assertEquals(0, Color.alpha(colors.inactivePrayerBgColor))
    }

    @Test
    fun borderIsIndependentAndExactlyMatchesAccent() {
        val colors = resolve(showBackground = false, showBorder = true)
        assertEquals(colors.accentColor, colors.rootBorderColor)
        assertEquals(0, Color.alpha(resolve(showBorder = false).rootBorderColor))
    }

    @Test
    fun transparentSurfaceDoesNotFillItsCenter() {
        val accent = 0xFF10B981.toInt()
        val bitmap = createWidgetSurfaceBitmap(
            widthPx = 100,
            heightPx = 80,
            fillColor = Color.TRANSPARENT,
            borderColor = accent,
            cornerRadiusPx = 16f,
            borderWidthPx = 3f
        )

        assertEquals(0, Color.alpha(bitmap.getPixel(50, 40)))
    }

}
