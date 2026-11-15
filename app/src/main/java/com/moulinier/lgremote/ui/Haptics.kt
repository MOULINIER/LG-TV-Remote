package com.moulinier.lgremote.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
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
 * Drives the vibrator directly so the in-app setting is what decides.
 *
 * Vibrations are tagged with an explicit (media) usage: with no usage, Android 12+
 * reclassifies short vibrations as "touch feedback" and drops them when the system's
 * touch-vibration setting is off, which is exactly what this setting should override.
 */
class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    private val audio = context.getSystemService(AudioManager::class.java)

    @Volatile var level: HapticLevel = HapticLevel.Medium

    /** A key press. */
    fun tap() = play(primary = true)

    /** A lighter tick for auto-repeat and scrolling. */
    fun tick() = play(primary = false)

    /** Plays a press at [level] regardless of the saved level (for the settings preview). */
    fun preview(level: HapticLevel) = play(primary = true, at = level)

    /** Why vibrations might not be felt, or null if nothing looks wrong. */
    fun problem(): String? = when {
        vibrator?.hasVibrator() != true -> "Aucun vibreur détecté sur ce téléphone."
        audio?.ringerMode == AudioManager.RINGER_MODE_SILENT ->
            "Le téléphone est en mode silencieux : Android y bloque les vibrations des applis. " +
                "Passez en mode vibreur ou sonnerie."
        else -> null
    }

    private fun play(primary: Boolean, at: HapticLevel = level) {
        val v = vibrator ?: return
        if (at == HapticLevel.Off || !v.hasVibrator()) return
        val effect = effect(v, at, primary) ?: return
        runCatching {
            when {
                Build.VERSION.SDK_INT >= 33 ->
                    v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
                else -> @Suppress("DEPRECATION") v.vibrate(
                    effect,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }
    }

    internal fun effect(v: Vibrator, at: HapticLevel, primary: Boolean): VibrationEffect? {
        // Device-tuned click effects (what keyboards use) where available.
        if (Build.VERSION.SDK_INT >= 29) {
            val id = when (at) {
                HapticLevel.Light -> VibrationEffect.EFFECT_TICK
                HapticLevel.Medium -> if (primary) VibrationEffect.EFFECT_CLICK else VibrationEffect.EFFECT_TICK
                HapticLevel.Strong -> if (primary) VibrationEffect.EFFECT_HEAVY_CLICK else VibrationEffect.EFFECT_CLICK
                HapticLevel.Off -> return null
            }
            return VibrationEffect.createPredefined(id)
        }
        // Older phones: one-shots long enough for slower vibration motors to spin up.
        val (ms, amp) = when (at) {
            HapticLevel.Light -> 20L to 90
            HapticLevel.Medium -> 30L to 180
            HapticLevel.Strong -> 45L to 255
            HapticLevel.Off -> return null
        }
        val duration = if (primary) ms else ms * 2 / 3
        val amplitude = if (v.hasAmplitudeControl()) amp else VibrationEffect.DEFAULT_AMPLITUDE
        return VibrationEffect.createOneShot(duration, amplitude)
    }
}

/** Null in previews/tests: callers simply skip vibration. */
val LocalHaptics = staticCompositionLocalOf<Haptics?> { null }
