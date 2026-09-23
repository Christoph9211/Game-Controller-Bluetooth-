package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HapticMotorChannel(val label: String) {
    LEFT_HEAVY("Left LRA (Heavy Low-Freq)"),
    RIGHT_LIGHT("Right LRA (Light High-Freq)"),
    DUAL_STEREO("Dual Stereo LRA Motors")
}

data class HapticTelemetryEvent(
    val eventName: String,
    val channel: HapticMotorChannel,
    val amplitudePct: Int,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

class HapticFeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val _lastHapticEvent = MutableStateFlow<HapticTelemetryEvent?>(null)
    val lastHapticEvent: StateFlow<HapticTelemetryEvent?> = _lastHapticEvent.asStateFlow()

    val isAvailable: Boolean
        get() = vibrator?.hasVibrator() == true

    val hasAmplitudeSupport: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.hasAmplitudeControl() == true
        } else {
            false
        }

    /**
     * Crisp tactile click for face buttons (A, B, X, Y), D-pad, and auxiliary buttons.
     */
    fun performButtonPress(enabled: Boolean, strength: Float = 0.85f, buttonName: String = "Button") {
        if (!enabled || !isAvailable) return

        val amp = calculateAmplitude(strength, base = 210)
        playWaveform(
            timings = longArrayOf(0, 14),
            amplitudes = intArrayOf(0, amp),
            predefinedFallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else null
        )
        recordEvent("$buttonName Click", HapticMotorChannel.RIGHT_LIGHT, amp, 14)
    }

    /**
     * Solid, snappy tactile click for shoulder bumpers (LB, RB).
     */
    fun performBumperPress(enabled: Boolean, strength: Float = 0.85f, bumperName: String = "LB") {
        if (!enabled || !isAvailable) return

        val amp = calculateAmplitude(strength, base = 230)
        playWaveform(
            timings = longArrayOf(0, 18),
            amplitudes = intArrayOf(0, amp),
            predefinedFallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_HEAVY_CLICK else null
        )
        recordEvent("$bumperName Microswitch", HapticMotorChannel.RIGHT_LIGHT, amp, 18)
    }

    /**
     * Subtle mechanical resistance tick as analog trigger travels through pressure points.
     */
    fun performTriggerStep(enabled: Boolean, strength: Float = 0.85f, triggerName: String = "LT") {
        if (!enabled || !isAvailable) return

        val amp = calculateAmplitude(strength, base = 110)
        playWaveform(
            timings = longArrayOf(0, 8),
            amplitudes = intArrayOf(0, amp),
            predefinedFallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else null
        )
        recordEvent("$triggerName Travel Notch", HapticMotorChannel.RIGHT_LIGHT, amp, 8)
    }

    /**
     * Firm mechanical bottom-out click when analog trigger is pulled to 100% squeeze.
     */
    fun performTriggerBottomOut(enabled: Boolean, strength: Float = 0.85f, triggerName: String = "LT") {
        if (!enabled || !isAvailable) return

        val amp = calculateAmplitude(strength, base = 255)
        playWaveform(
            timings = longArrayOf(0, 22),
            amplitudes = intArrayOf(0, amp),
            predefinedFallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_HEAVY_CLICK else null
        )
        recordEvent("$triggerName Bottom-Out Snap", HapticMotorChannel.DUAL_STEREO, amp, 22)
    }

    /**
     * Thumbstick click (L3 or R3) deep mechanical thud.
     */
    fun performStickClick(enabled: Boolean, strength: Float = 0.85f, stickName: String = "L3") {
        if (!enabled || !isAvailable) return

        val amp = calculateAmplitude(strength, base = 240)
        playWaveform(
            timings = longArrayOf(0, 26),
            amplitudes = intArrayOf(0, amp),
            predefinedFallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_HEAVY_CLICK else null
        )
        recordEvent("$stickName Thumbstick Press", HapticMotorChannel.LEFT_HEAVY, amp, 26)
    }

    /**
     * Subtle haptic click when thumbstick passes through or returns to center deadzone.
     */
    fun performStickCenterSnap(enabled: Boolean, strength: Float = 0.85f) {
        if (!enabled || !isAvailable) return

        val amp = calculateAmplitude(strength, base = 90)
        playWaveform(
            timings = longArrayOf(0, 8),
            amplitudes = intArrayOf(0, amp),
            predefinedFallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else null
        )
        recordEvent("Stick Deadzone Centering", HapticMotorChannel.RIGHT_LIGHT, amp, 8)
    }

    /**
     * Outer boundary collision bump when thumbstick hits outer ring.
     */
    fun performStickPerimeterBump(enabled: Boolean, strength: Float = 0.85f) {
        if (!enabled || !isAvailable) return

        val amp = calculateAmplitude(strength, base = 175)
        playWaveform(
            timings = longArrayOf(0, 16),
            amplitudes = intArrayOf(0, amp),
            predefinedFallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else null
        )
        recordEvent("Stick Perimeter Boundary", HapticMotorChannel.LEFT_HEAVY, amp, 16)
    }

    /**
     * Rapid staccato fire pulse when Turbo Mode is firing.
     */
    fun performTurboPulse(enabled: Boolean, strength: Float = 0.85f) {
        if (!enabled || !isAvailable) return

        val amp = calculateAmplitude(strength, base = 190)
        playWaveform(
            timings = longArrayOf(0, 10, 15, 10),
            amplitudes = intArrayOf(0, amp, 0, amp),
            predefinedFallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_DOUBLE_CLICK else null
        )
        recordEvent("Turbo Auto-Fire Burst", HapticMotorChannel.DUAL_STEREO, amp, 35)
    }

    /**
     * Full dual LRA rumble motor simulation with frequency and amplitude envelope.
     */
    fun performMotorRumble(
        enabled: Boolean,
        channel: HapticMotorChannel,
        strength: Float = 0.85f,
        durationMs: Long = 120L
    ) {
        if (!enabled || !isAvailable) return

        when (channel) {
            HapticMotorChannel.LEFT_HEAVY -> {
                // Low-frequency deep rumble: longer pulses, high mass
                val amp = calculateAmplitude(strength, base = 245)
                val timings = longArrayOf(0, 40, 20, 45, 20, 50)
                val amps = intArrayOf(0, amp, 0, (amp * 0.9f).toInt(), 0, amp)
                playWaveform(timings, amps, null)
                recordEvent("Low-Frequency Heavy LRA", channel, amp, 175)
            }
            HapticMotorChannel.RIGHT_LIGHT -> {
                // High-frequency buzzing trigger motor: rapid bursts
                val amp = calculateAmplitude(strength, base = 150)
                val timings = longArrayOf(0, 16, 12, 16, 12, 16)
                val amps = intArrayOf(0, amp, 0, (amp * 0.8f).toInt(), 0, amp)
                playWaveform(timings, amps, null)
                recordEvent("High-Frequency Light LRA", channel, amp, 72)
            }
            HapticMotorChannel.DUAL_STEREO -> {
                // Stereo impact: rising envelope
                val amp = calculateAmplitude(strength, base = 255)
                val timings = longArrayOf(0, 25, 15, 45, 15, 65)
                val amps = intArrayOf(0, (amp * 0.7f).toInt(), 0, (amp * 0.9f).toInt(), 0, amp)
                playWaveform(timings, amps, null)
                recordEvent("Dual Motor Stereo Impulse", channel, amp, 165)
            }
        }
    }

    /**
     * UI control tick (toggles, tabs, drawer actions).
     */
    fun performUiTick(enabled: Boolean, strength: Float = 0.85f) {
        if (!enabled || !isAvailable) return

        val amp = calculateAmplitude(strength, base = 100)
        playWaveform(
            timings = longArrayOf(0, 10),
            amplitudes = intArrayOf(0, amp),
            predefinedFallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else null
        )
        recordEvent("UI Micro-Feedback", HapticMotorChannel.RIGHT_LIGHT, amp, 10)
    }

    private fun calculateAmplitude(strength: Float, base: Int): Int {
        val clampedStrength = strength.coerceIn(0.1f, 1.0f)
        return (base * clampedStrength).toInt().coerceIn(1, 255)
    }

    private fun playWaveform(
        timings: LongArray,
        amplitudes: IntArray,
        predefinedFallback: Int?
    ) {
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (hasAmplitudeSupport) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vib.vibrate(effect)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && predefinedFallback != null) {
                    val effect = VibrationEffect.createPredefined(predefinedFallback)
                    vib.vibrate(effect)
                } else {
                    val totalDuration = timings.sum().coerceAtLeast(10L)
                    val effect = VibrationEffect.createOneShot(totalDuration, VibrationEffect.DEFAULT_AMPLITUDE)
                    vib.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                val totalDuration = timings.sum().coerceAtLeast(10L)
                @Suppress("DEPRECATION")
                vib.vibrate(totalDuration)
            }
        } catch (e: Exception) {
            // Gracefully handle hardware without full vibration permission or silent mode
        }
    }

    private fun recordEvent(name: String, channel: HapticMotorChannel, amplitude: Int, durationMs: Long) {
        val pct = ((amplitude / 255f) * 100).toInt()
        _lastHapticEvent.value = HapticTelemetryEvent(
            eventName = name,
            channel = channel,
            amplitudePct = pct,
            durationMs = durationMs
        )
    }
}
