package com.moulinier.lgremote

import android.app.Application
import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.moulinier.lgremote.ui.HapticLevel
import com.moulinier.lgremote.ui.Haptics
import com.moulinier.lgremote.ui.IconKey
import com.moulinier.lgremote.ui.LocalHaptics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.CompositionLocalProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowVibrator

/** A press on a remote key must reach the vibrator, and "Off" must keep it silent. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HapticsTest {
    @get:Rule val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Suppress("DEPRECATION")
    private val vibrator get() = shadowOf(app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)

    private fun pressKey(level: HapticLevel) {
        ShadowVibrator.reset()
        vibrator.setHasVibrator(true)
        val haptics = Haptics(app).also { it.level = level }
        compose.setContent {
            CompositionLocalProvider(LocalHaptics provides haptics) {
                IconKey(Icons.Rounded.Home, "Accueil", {})
            }
        }
        compose.onNodeWithContentDescription("Accueil").performTouchInput { down(center); up() }
        compose.waitForIdle()
    }

    @Test fun keyPressReachesVibrator() {
        pressKey(HapticLevel.Medium)
        // An explicit usage, so Android does not treat it as (possibly disabled) touch feedback.
        val attrs = vibrator.vibrationAttributesFromLastVibration as VibrationAttributes
        assertEquals(VibrationAttributes.USAGE_MEDIA, attrs.usage)
    }

    @Test fun levelsMapToDeviceClickEffects() {
        @Suppress("DEPRECATION")
        val v = app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val h = Haptics(app)
        fun e(id: Int) = VibrationEffect.createPredefined(id)
        assertEquals(e(VibrationEffect.EFFECT_TICK), h.effect(v, HapticLevel.Light, true))
        assertEquals(e(VibrationEffect.EFFECT_CLICK), h.effect(v, HapticLevel.Medium, true))
        assertEquals(e(VibrationEffect.EFFECT_HEAVY_CLICK), h.effect(v, HapticLevel.Strong, true))
        assertEquals(e(VibrationEffect.EFFECT_CLICK), h.effect(v, HapticLevel.Strong, false))
        assertNull(h.effect(v, HapticLevel.Off, true))
    }

    @Test fun offStaysSilent() {
        pressKey(HapticLevel.Off)
        assertNull(vibrator.vibrationAttributesFromLastVibration)
    }
}
