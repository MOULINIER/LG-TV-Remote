package com.moulinier.lgremote.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf

/** User-selectable vibration strength for remote keys. */
enum class HapticLevel(val label: String) {
    Off("Désactivé"), Light("Léger"), Medium("Moyen"), Strong("Fort");

    companion object {
        fun parse(name: String?) = entries.firstOrNull { it.name == name } ?: Medium
    }
}

/**
 * Drives the vibrator directly (instead of View.performHapticFeedback) so the user's
 * choice in the app applies regardless of the system "touch feedback" setting.
 */
class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    @Volatile var level: HapticLevel = HapticLevel.Medium

    /** A key press. */
    fun tap() = vibrate(strong = true)

    /** A lighter tick for auto-repeat and scrolling. */
    fun tick() = vibrate(strong = false)

    private fun vibrate(strong: Boolean) {
        val v = vibrator ?: return
        if (level == HapticLevel.Off || !v.hasVibrator()) return
        val (ms, amp) = when (level) {
            HapticLevel.Light -> 8L to 50
            HapticLevel.Medium -> 14L to 130
            HapticLevel.Strong -> 24L to 255
            HapticLevel.Off -> return
        }
        val duration = if (strong) ms else (ms * 2 / 3).coerceAtLeast(5)
        val amplitude = if (!v.hasAmplitudeControl()) VibrationEffect.DEFAULT_AMPLITUDE
            else if (strong) amp else (amp * 2 / 3).coerceAtLeast(1)
        runCatching { v.vibrate(VibrationEffect.createOneShot(duration, amplitude)) }
    }
}

/** Null in previews/tests: callers simply skip vibration. */
val LocalHaptics = staticCompositionLocalOf<Haptics?> { null }
