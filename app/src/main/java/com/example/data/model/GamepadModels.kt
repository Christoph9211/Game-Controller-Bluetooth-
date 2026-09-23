package com.example.data.model

enum class ControllerElementId(val displayName: String, val category: String) {
    LT_LB("LT / LB", "Bumpers & Triggers"),
    LEFT_STICK("Left Stick", "Analog Stick"),
    DPAD("D-Pad", "Directional Pad"),
    AUX_BUTTONS("Select / Start", "System"),
    RT_RB("RT / RB", "Bumpers & Triggers"),
    RIGHT_STICK("Right Stick", "Analog Stick"),
    ABXY("ABXY Cluster", "Action Buttons"),
    PADDLE_P1("Paddle P1", "Rear Paddles"),
    PADDLE_P2("Paddle P2", "Rear Paddles"),
    TURBO_BTN("Turbo / Rapid", "Special Actions")
}

enum class StickStylePreset(val label: String) {
    HALO("Halo"),
    TARGET("Target"),
    MINIMAL("Minimal")
}

data class ElementLayoutConfig(
    val elementId: ControllerElementId,
    val xPercent: Float, // 0 to 100
    val yPercent: Float, // 0 to 100
    val scale: Float = 1.0f, // 0.8 to 1.4
    val stylePreset: StickStylePreset = StickStylePreset.HALO
)

data class ControllerLayoutProfile(
    val id: String = "default_asymmetric",
    val name: String = "Asymmetric Offset (Default)",
    val isCustom: Boolean = false,
    val elements: Map<ControllerElementId, ElementLayoutConfig> = defaultLayoutElements()
) {
    companion object {
        fun defaultLayoutElements(): Map<ControllerElementId, ElementLayoutConfig> = mapOf(
            ControllerElementId.LT_LB to ElementLayoutConfig(
                elementId = ControllerElementId.LT_LB,
                xPercent = 6f,
                yPercent = 6f,
                scale = 1.0f
            ),
            ControllerElementId.LEFT_STICK to ElementLayoutConfig(
                elementId = ControllerElementId.LEFT_STICK,
                xPercent = 7f,
                yPercent = 48f,
                scale = 1.0f,
                stylePreset = StickStylePreset.HALO
            ),
            ControllerElementId.DPAD to ElementLayoutConfig(
                elementId = ControllerElementId.DPAD,
                xPercent = 27f,
                yPercent = 48f,
                scale = 1.0f
            ),
            ControllerElementId.AUX_BUTTONS to ElementLayoutConfig(
                elementId = ControllerElementId.AUX_BUTTONS,
                xPercent = 49f,
                yPercent = 38f,
                scale = 1.0f
            ),
            ControllerElementId.RT_RB to ElementLayoutConfig(
                elementId = ControllerElementId.RT_RB,
                xPercent = 85f,
                yPercent = 6f,
                scale = 1.0f
            ),
            ControllerElementId.RIGHT_STICK to ElementLayoutConfig(
                elementId = ControllerElementId.RIGHT_STICK,
                xPercent = 63f,
                yPercent = 46f,
                scale = 1.0f,
                stylePreset = StickStylePreset.HALO
            ),
            ControllerElementId.ABXY to ElementLayoutConfig(
                elementId = ControllerElementId.ABXY,
                xPercent = 84f,
                yPercent = 48f,
                scale = 1.0f
            )
        )

        fun fpsLayoutElements(): Map<ControllerElementId, ElementLayoutConfig> = mapOf(
            ControllerElementId.LT_LB to ElementLayoutConfig(ControllerElementId.LT_LB, 6f, 6f, 1.05f),
            ControllerElementId.RT_RB to ElementLayoutConfig(ControllerElementId.RT_RB, 85f, 6f, 1.05f),
            ControllerElementId.LEFT_STICK to ElementLayoutConfig(ControllerElementId.LEFT_STICK, 8f, 44f, 1.1f),
            ControllerElementId.RIGHT_STICK to ElementLayoutConfig(ControllerElementId.RIGHT_STICK, 66f, 48f, 1.15f),
            ControllerElementId.DPAD to ElementLayoutConfig(ControllerElementId.DPAD, 26f, 54f, 0.95f),
            ControllerElementId.ABXY to ElementLayoutConfig(ControllerElementId.ABXY, 86f, 38f, 1.05f),
            ControllerElementId.AUX_BUTTONS to ElementLayoutConfig(ControllerElementId.AUX_BUTTONS, 48f, 28f, 0.95f),
            ControllerElementId.PADDLE_P1 to ElementLayoutConfig(ControllerElementId.PADDLE_P1, 14f, 76f, 1.0f),
            ControllerElementId.PADDLE_P2 to ElementLayoutConfig(ControllerElementId.PADDLE_P2, 78f, 76f, 1.0f)
        )

        fun arcadeFightingLayoutElements(): Map<ControllerElementId, ElementLayoutConfig> = mapOf(
            ControllerElementId.LT_LB to ElementLayoutConfig(ControllerElementId.LT_LB, 8f, 8f, 1.0f),
            ControllerElementId.RT_RB to ElementLayoutConfig(ControllerElementId.RT_RB, 84f, 8f, 1.0f),
            ControllerElementId.DPAD to ElementLayoutConfig(ControllerElementId.DPAD, 14f, 44f, 1.25f),
            ControllerElementId.ABXY to ElementLayoutConfig(ControllerElementId.ABXY, 76f, 42f, 1.3f),
            ControllerElementId.TURBO_BTN to ElementLayoutConfig(ControllerElementId.TURBO_BTN, 60f, 26f, 1.0f),
            ControllerElementId.AUX_BUTTONS to ElementLayoutConfig(ControllerElementId.AUX_BUTTONS, 48f, 16f, 1.0f),
            ControllerElementId.LEFT_STICK to ElementLayoutConfig(ControllerElementId.LEFT_STICK, 30f, 64f, 0.95f),
            ControllerElementId.RIGHT_STICK to ElementLayoutConfig(ControllerElementId.RIGHT_STICK, 54f, 64f, 0.95f)
        )

        fun southpawLayoutElements(): Map<ControllerElementId, ElementLayoutConfig> = mapOf(
            ControllerElementId.LT_LB to ElementLayoutConfig(ControllerElementId.LT_LB, 6f, 6f, 1.0f),
            ControllerElementId.RT_RB to ElementLayoutConfig(ControllerElementId.RT_RB, 85f, 6f, 1.0f),
            ControllerElementId.LEFT_STICK to ElementLayoutConfig(ControllerElementId.LEFT_STICK, 66f, 46f, 1.0f),
            ControllerElementId.RIGHT_STICK to ElementLayoutConfig(ControllerElementId.RIGHT_STICK, 8f, 48f, 1.0f),
            ControllerElementId.DPAD to ElementLayoutConfig(ControllerElementId.DPAD, 84f, 48f, 1.0f),
            ControllerElementId.ABXY to ElementLayoutConfig(ControllerElementId.ABXY, 28f, 48f, 1.0f),
            ControllerElementId.AUX_BUTTONS to ElementLayoutConfig(ControllerElementId.AUX_BUTTONS, 48f, 38f, 1.0f)
        )
    }
}

enum class DeviceType {
    PC,
    LAPTOP,
    STEAM_DECK,
    CONSOLE,
    GAMEPAD
}

data class DeviceTarget(
    val id: String,
    val name: String,
    val subtitle: String,
    val type: DeviceType,
    val connectionType: String,
    val rssiDbm: Int,
    val isPaired: Boolean,
    val isConnected: Boolean,
    val streamRate: String,
    val lastSeenOrConnected: String = "Just now",
    val batteryPct: Int? = null,
    val macAddress: String = "7C:BB:8A:2F:10:9A",
    val protocol: String = "Bluetooth HID"
)

data class TelemetryData(
    val linkState: String = "HID Connected",
    val roundtripMs: Float = 3.8f,
    val samplingHz: Int = 125,
    val hostName: String = "Custom Rig (RTX 4090)",
    val hostMac: String = "94:E6:F7:2B:90:1C",
    val profileName: String = "P1 · Low Latency FPS",
    val rfDbm: Int = -42,
    val signalIntegrity: Int = 98,
    val packetLoss: Float = 0.00f,
    val minLatency: Float = 2.4f,
    val maxLatency: Float = 5.1f,
    val jitterMs: Float = 0.4f,
    val hallDrift: Float = 0.0f,
    val ltPressure: Int = 0, // 0..255
    val rtPressure: Int = 0, // 0..255
    val thermalC: Float = 29.4f,
    val txRate: Float = 124.9f,
    val totalPackets: Long = 243184L
)

data class QuickActionsSettings(
    val currentProfile: String = "P1: Low Latency FPS",
    val hapticsEnabled: Boolean = true,
    val hapticStrength: Float = 0.85f,
    val hapticProfile: String = "Crisp Mechanical", // "Crisp Mechanical", "Heavy Dual LRA", "Subtle Micro-Tick"
    val triggerResistanceHaptic: Boolean = true,
    val stickPerimeterHaptic: Boolean = true,
    val gyroAimEnabled: Boolean = false,
    val turboEnabled: Boolean = false,
    val deadzonePct: Int = 5,
    val pollRateHz: Int = 250,
    val audioHapticSync: Boolean = true
)

enum class GamepadScreen {
    CONTROLLER,
    CUSTOMIZE_LAYOUT,
    DEVICE_DISCOVERY,
    DIAGNOSTICS
}

data class PingBurstResult(
    val packetCount: Int = 100,
    val minLatencyMs: Float = 2.4f,
    val avgLatencyMs: Float = 3.6f,
    val maxLatencyMs: Float = 4.9f,
    val packetLossPct: Float = 0.0f,
    val jitterMs: Float = 0.29f,
    val qualityGrade: String = "EXCELLENT",
    val timestamp: String = "Just now"
)

enum class ConnectionQualityRating(
    val label: String,
    val description: String,
    val minScore: Int
) {
    EXCELLENT("EXCELLENT", "Competitive Esports Grade • Ultra Low Jitter", 85),
    GOOD("GOOD", "Solid Link • Smooth Casual Gameplay", 70),
    FAIR("FAIR", "Marginal RF Margin • Potential Jitter Spikes", 50),
    POOR("POOR", "Degraded Signal • Packet Retransmissions", 0)
}
