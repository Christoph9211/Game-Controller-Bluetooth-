package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionQualityRating
import com.example.data.model.DeviceTarget
import com.example.data.model.DeviceType
import com.example.data.model.PingBurstResult
import com.example.data.model.TelemetryData
import com.example.ui.theme.ActiveControlFill
import com.example.ui.theme.ControlBorderGlow
import com.example.ui.theme.ControlBorderSubtle
import com.example.ui.theme.OnPrimaryBlue
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryContainerBlue
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SurfaceCanvas
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow
import com.example.ui.theme.SurfaceContainerLowest
import com.example.ui.theme.SurfaceControl
import com.example.ui.theme.SurfaceControlRaised
import com.example.ui.theme.SurfaceDefault
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.XboxBlueX
import com.example.ui.theme.XboxGreenA
import com.example.ui.theme.XboxYellowY
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Real-time Signal Strength (RSSI) and Latency Monitor Dashboard
 * for evaluating connection quality of paired Bluetooth gamepads.
 */
@Composable
fun RssiLatencyMonitorDashboard(
    telemetry: TelemetryData,
    rssiHistory: List<Int>,
    latencyHistory: List<Float>,
    pairedGamepads: List<DeviceTarget>,
    selectedGamepadId: String,
    isPingBurstRunning: Boolean,
    pingBurstProgress: Float,
    pingBurstResult: PingBurstResult?,
    onSelectGamepad: (String) -> Unit,
    onRunPingBurstTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Compute quality score based on current RSSI and Latency
    val currentDbm = telemetry.rfDbm
    val currentLatency = telemetry.roundtripMs
    val qualityScore = computeConnectionQualityScore(currentDbm, currentLatency)
    val qualityRating = when {
        qualityScore >= 85 -> ConnectionQualityRating.EXCELLENT
        qualityScore >= 70 -> ConnectionQualityRating.GOOD
        qualityScore >= 50 -> ConnectionQualityRating.FAIR
        else -> ConnectionQualityRating.POOR
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDefault)
            .border(1.dp, ControlBorderSubtle, RoundedCornerShape(16.dp))
            .padding(18.dp)
            .testTag("rssi_latency_monitor_dashboard"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Dashboard Header: Title + Real-time Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ActiveControlFill)
                        .border(1.dp, ControlBorderGlow, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Signal & Latency Monitor",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "REAL-TIME RSSI & LATENCY MONITOR",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        LiveTelemetryPulsingBadge()
                    }
                    Text(
                        text = "Bluetooth Low Energy HID Link Quality & Packet Transmission Health",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            // Quality score chip
            ConnectionQualityScoreChip(rating = qualityRating, score = qualityScore)
        }

        // 2. Paired Gamepad Selector Ribbon
        PairedGamepadsSelectorRibbon(
            pairedGamepads = pairedGamepads,
            selectedId = selectedGamepadId,
            onSelect = onSelectGamepad
        )

        // 3. Hero Metric Cards Grid (Score Gauge, Live RSSI, Live Latency, Packet Loss)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero 1: Quality Dial Card
            QualityScoreGaugeCard(
                score = qualityScore,
                rating = qualityRating,
                modifier = Modifier.weight(1f)
            )

            // Hero 2: Live RSSI Signal Gauge Card
            LiveRssiGaugeCard(
                rfDbm = currentDbm,
                integrity = telemetry.signalIntegrity,
                modifier = Modifier.weight(1f)
            )

            // Hero 3: Live Input Latency Gauge Card
            LiveLatencyGaugeCard(
                roundtripMs = currentLatency,
                jitterMs = telemetry.jitterMs,
                samplingHz = telemetry.samplingHz,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Real-time Rolling Waveform Charts: RSSI Timeline & Latency Timeline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Chart: RSSI Timeline Graph
            RealTimeRssiTimelineCard(
                rssiHistory = rssiHistory,
                currentDbm = currentDbm,
                modifier = Modifier.weight(1f)
            )

            // Right Chart: Latency Timeline Graph
            RealTimeLatencyTimelineCard(
                latencyHistory = latencyHistory,
                currentLatency = currentLatency,
                jitterMs = telemetry.jitterMs,
                modifier = Modifier.weight(1f)
            )
        }

        // 5. Connection Stress Test / Ping Burst Diagnostics Bar
        PingBurstStressTestBar(
            isRunning = isPingBurstRunning,
            progress = pingBurstProgress,
            result = pingBurstResult,
            onRunTest = onRunPingBurstTest
        )
    }
}

/**
 * Computes a composite connection quality score (0 to 100).
 */
private fun computeConnectionQualityScore(rfDbm: Int, roundtripMs: Float): Int {
    // RSSI contribution (0 to 60 points): -35 dBm is 60, -85 dBm is 10
    val rssiFactor = ((rfDbm - (-85f)) / 50f).coerceIn(0f, 1f)
    val rssiScore = rssiFactor * 60f

    // Latency contribution (0 to 40 points): 2ms is 40, 15ms is 5
    val latencyFactor = (1f - ((roundtripMs - 2f) / 13f)).coerceIn(0f, 1f)
    val latencyScore = latencyFactor * 40f

    return (rssiScore + latencyScore).roundToInt().coerceIn(10, 100)
}

/**
 * Pulsing Green Live Telemetry Badge
 */
@Composable
private fun LiveTelemetryPulsingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(XboxGreenA.copy(alpha = 0.15f))
            .border(1.dp, XboxGreenA.copy(alpha = alpha), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(XboxGreenA)
        )
        Text(
            text = "LIVE 1000Hz MONITOR",
            color = XboxGreenA,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Quality Score Status Chip
 */
@Composable
private fun ConnectionQualityScoreChip(rating: ConnectionQualityRating, score: Int) {
    val accentColor = when (rating) {
        ConnectionQualityRating.EXCELLENT -> XboxGreenA
        ConnectionQualityRating.GOOD -> PrimaryContainerBlue
        ConnectionQualityRating.FAIR -> XboxYellowY
        ConnectionQualityRating.POOR -> StatusError
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .border(1.dp, accentColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (rating == ConnectionQualityRating.EXCELLENT) Icons.Default.CheckCircle else Icons.Default.Sensors,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "${rating.label} ($score%)",
            color = accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Paired Gamepads Selector Ribbon
 */
@Composable
private fun PairedGamepadsSelectorRibbon(
    pairedGamepads: List<DeviceTarget>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "MONITORED GAMEPAD TARGET",
            color = TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            pairedGamepads.forEach { gamepad ->
                val isSelected = gamepad.id == selectedId
                val isConnected = gamepad.isConnected

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ActiveControlFill else SurfaceCard)
                        .border(
                            1.dp,
                            if (isSelected) ControlBorderGlow else ControlBorderSubtle,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(gamepad.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("gamepad_monitor_item_${gamepad.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = gamepad.name,
                        tint = if (isSelected) PrimaryBlue else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = gamepad.name,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )

                            if (isConnected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(XboxGreenA)
                                )
                            }
                        }

                        Text(
                            text = "${gamepad.rssiDbm} dBm • ${gamepad.protocol}",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hero Card 1: Quality Score Radial Gauge
 */
@Composable
private fun QualityScoreGaugeCard(
    score: Int,
    rating: ConnectionQualityRating,
    modifier: Modifier = Modifier
) {
    val accentColor = when (rating) {
        ConnectionQualityRating.EXCELLENT -> XboxGreenA
        ConnectionQualityRating.GOOD -> PrimaryContainerBlue
        ConnectionQualityRating.FAIR -> XboxYellowY
        ConnectionQualityRating.POOR -> StatusError
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CONNECTION QUALITY",
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            // Radial Arc Gauge Canvas
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 6.dp.toPx()
                    val radius = (size.minDimension - strokeW) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Track arc
                    drawArc(
                        color = SurfaceControlRaised,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Active Score Arc
                    val sweep = 270f * (score / 100f)
                    drawArc(
                        color = accentColor,
                        startAngle = 135f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score%",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "STABILITY",
                        color = TextTertiary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = rating.description,
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Hero Card 2: Real-time RSSI Signal Gauge Card
 */
@Composable
private fun LiveRssiGaugeCard(
    rfDbm: Int,
    integrity: Int,
    modifier: Modifier = Modifier
) {
    val signalBars = when {
        rfDbm >= -48 -> 5
        rfDbm >= -60 -> 4
        rfDbm >= -70 -> 3
        rfDbm >= -80 -> 2
        else -> 1
    }

    val statusColor = when {
        rfDbm >= -55 -> XboxGreenA
        rfDbm >= -70 -> PrimaryBlue
        rfDbm >= -80 -> XboxYellowY
        else -> StatusError
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SIGNAL STRENGTH",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                // 5-bar meter
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((i * 3 + 3).dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (i <= signalBars) statusColor else SurfaceControlRaised)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$rfDbm",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "dBm",
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            // Integrity Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("RF Link Integrity", color = TextTertiary, fontSize = 9.sp)
                    Text("$integrity%", color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { integrity / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = statusColor,
                    trackColor = SurfaceControlRaised
                )
            }

            Text(
                text = "Estimated Range: ~${estimateDistanceMeters(rfDbm)}m Line-of-sight",
                color = TextTertiary,
                fontSize = 9.sp
            )
        }
    }
}

/**
 * Estimates distance in meters from RSSI dBm.
 */
private fun estimateDistanceMeters(rfDbm: Int): String {
    val meters = when {
        rfDbm >= -45 -> 0.8f
        rfDbm >= -55 -> 1.5f
        rfDbm >= -68 -> 2.6f
        rfDbm >= -78 -> 4.2f
        else -> 6.5f
    }
    return String.format("%.1f", meters)
}

/**
 * Hero Card 3: Live Input Latency Gauge Card
 */
@Composable
private fun LiveLatencyGaugeCard(
    roundtripMs: Float,
    jitterMs: Float,
    samplingHz: Int,
    modifier: Modifier = Modifier
) {
    val latencyColor = when {
        roundtripMs <= 4.2f -> XboxGreenA
        roundtripMs <= 7.5f -> PrimaryBlue
        roundtripMs <= 12f -> XboxYellowY
        else -> StatusError
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INPUT LATENCY",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = latencyColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = String.format("%.1f", roundtripMs),
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ms",
                    color = latencyColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            // Jitter & Polling row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Jitter Variance", color = TextTertiary, fontSize = 9.sp)
                    Text("±${String.format("%.2f", jitterMs)} ms", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Polling Rate", color = TextTertiary, fontSize = 9.sp)
                    Text("$samplingHz Hz", color = XboxGreenA, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Target Polling Window: < 4.0 ms",
                color = TextTertiary,
                fontSize = 9.sp
            )
        }
    }
}

/**
 * Real-time Rolling Waveform: RSSI Timeline Card
 */
@Composable
private fun RealTimeRssiTimelineCard(
    rssiHistory: List<Int>,
    currentDbm: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(XboxGreenA)
                    )
                    Text(
                        text = "RSSI SIGNAL TIMELINE (dBm)",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Current: $currentDbm dBm",
                    color = XboxGreenA,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Canvas Waveform Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCanvas)
                    .border(1.dp, SurfaceControlRaised, RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRssiWaveform(rssiHistory)
                }
            }

            // Legend Ticks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("-30 dBm (Max)", color = TextTertiary, fontSize = 9.sp)
                Text("-55 dBm (Good)", color = TextTertiary, fontSize = 9.sp)
                Text("-85 dBm (Floor)", color = TextTertiary, fontSize = 9.sp)
            }
        }
    }
}

/**
 * Draws the RSSI timeline waveform with gradient fill and threshold guides.
 */
private fun DrawScope.drawRssiWaveform(rssiHistory: List<Int>) {
    if (rssiHistory.isEmpty()) return

    val w = size.width
    val h = size.height
    val minDbm = -85f
    val maxDbm = -30f
    val range = maxDbm - minDbm

    // Horizontal Guideline (-55 dBm threshold)
    val guideY = h - (( -55f - minDbm) / range) * h
    drawLine(
        color = ControlBorderSubtle.copy(alpha = 0.5f),
        start = Offset(0f, guideY),
        end = Offset(w, guideY),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    )

    // Build Line Path
    val points = rssiHistory.mapIndexed { index, dbm ->
        val x = (index.toFloat() / (rssiHistory.size - 1).coerceAtLeast(1)) * w
        val normalized = ((dbm.toFloat() - minDbm) / range).coerceIn(0.05f, 0.95f)
        val y = h - (normalized * h)
        Offset(x, y)
    }

    val strokePath = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val cx = (prev.x + curr.x) / 2f
            quadraticTo(prev.x, prev.y, cx, (prev.y + curr.y) / 2f)
        }
        lineTo(points.last().x, points.last().y)
    }

    // Fill Path
    val fillPath = Path().apply {
        addPath(strokePath)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }

    // Draw Fill
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            listOf(XboxGreenA.copy(alpha = 0.25f), Color.Transparent),
            startY = 0f,
            endY = h
        )
    )

    // Draw Stroke
    drawPath(
        path = strokePath,
        color = XboxGreenA,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Draw Latest Point Glowing Circle
    val latest = points.last()
    drawCircle(color = XboxGreenA.copy(alpha = 0.4f), radius = 6.dp.toPx(), center = latest)
    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = latest)
}

/**
 * Real-time Rolling Waveform: Latency Timeline Card
 */
@Composable
private fun RealTimeLatencyTimelineCard(
    latencyHistory: List<Float>,
    currentLatency: Float,
    jitterMs: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainerBlue)
                    )
                    Text(
                        text = "LATENCY & JITTER GRAPH (ms)",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${String.format("%.1f", currentLatency)} ms",
                    color = PrimaryBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Canvas Waveform Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCanvas)
                    .border(1.dp, SurfaceControlRaised, RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLatencyWaveform(latencyHistory)
                }
            }

            // Legend Ticks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 ms (Ideal)", color = TextTertiary, fontSize = 9.sp)
                Text("4.0 ms (Target)", color = TextTertiary, fontSize = 9.sp)
                Text("10.0 ms (Threshold)", color = TextTertiary, fontSize = 9.sp)
            }
        }
    }
}

/**
 * Draws the Latency timeline waveform with cyan gradient fill and 4.0ms target guide.
 */
private fun DrawScope.drawLatencyWaveform(latencyHistory: List<Float>) {
    if (latencyHistory.isEmpty()) return

    val w = size.width
    val h = size.height
    val minMs = 0f
    val maxMs = 12f
    val range = maxMs - minMs

    // Target guideline (4.0 ms)
    val guideY = h - ((4.0f - minMs) / range) * h
    drawLine(
        color = XboxGreenA.copy(alpha = 0.6f),
        start = Offset(0f, guideY),
        end = Offset(w, guideY),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
    )

    // Build Line Path
    val points = latencyHistory.mapIndexed { index, ms ->
        val x = (index.toFloat() / (latencyHistory.size - 1).coerceAtLeast(1)) * w
        val normalized = ((ms - minMs) / range).coerceIn(0.05f, 0.95f)
        val y = h - (normalized * h)
        Offset(x, y)
    }

    val strokePath = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val cx = (prev.x + curr.x) / 2f
            quadraticTo(prev.x, prev.y, cx, (prev.y + curr.y) / 2f)
        }
        lineTo(points.last().x, points.last().y)
    }

    // Fill Path
    val fillPath = Path().apply {
        addPath(strokePath)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }

    // Draw Fill
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            listOf(PrimaryContainerBlue.copy(alpha = 0.25f), Color.Transparent),
            startY = 0f,
            endY = h
        )
    )

    // Draw Stroke
    drawPath(
        path = strokePath,
        color = PrimaryContainerBlue,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Draw Latest Point
    val latest = points.last()
    drawCircle(color = PrimaryContainerBlue.copy(alpha = 0.5f), radius = 6.dp.toPx(), center = latest)
    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = latest)
}

/**
 * Connection Stress & Ping Burst Test Bar
 */
@Composable
private fun PingBurstStressTestBar(
    isRunning: Boolean,
    progress: Float,
    result: PingBurstResult?,
    onRunTest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, ControlBorderSubtle.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = XboxYellowY,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "BURST PACKET STRESS TEST (100 HID PACKETS)",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onRunTest,
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainerBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("run_ping_burst_test_btn")
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testing...", fontSize = 11.sp)
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Stress Test", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Progress Bar when running
            if (isRunning) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Broadcasting 100 high-frequency HID state packets...", color = TextSecondary, fontSize = 10.sp)
                        Text("${(progress * 100).roundToInt()}%", color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryContainerBlue,
                        trackColor = SurfaceControlRaised
                    )
                }
            } else if (result != null) {
                // Result readout strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCard)
                        .border(1.dp, XboxGreenA.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ResultMetric("Packets Sent/Recv", "${result.packetCount}/${result.packetCount}")
                        ResultMetric("Packet Loss", "${result.packetLossPct}%", color = XboxGreenA)
                        ResultMetric("Min Ping", "${result.minLatencyMs}ms")
                        ResultMetric("Avg Ping", "${result.avgLatencyMs}ms", color = PrimaryBlue)
                        ResultMetric("Max Ping", "${result.maxLatencyMs}ms")
                        ResultMetric("Jitter", "±${result.jitterMs}ms")
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(XboxGreenA.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = result.qualityGrade,
                            color = XboxGreenA,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Initiate an instantaneous 100-packet high-frequency transmission burst to measure queue latency variance under peak gaming input loads.",
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun ResultMetric(label: String, value: String, color: Color = TextPrimary) {
    Column {
        Text(text = label, color = TextTertiary, fontSize = 8.sp)
        Text(text = value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
