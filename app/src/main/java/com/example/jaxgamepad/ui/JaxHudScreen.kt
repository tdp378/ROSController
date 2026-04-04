package com.example.jaxgamepad.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaxgamepad.Black
import com.example.jaxgamepad.R
import com.example.jaxgamepad.RobotMode
import kotlin.math.min
import kotlin.math.sqrt

// Enum for LED and Telemetry selection
enum class HudIndicator { ROS_LINK, MOTORS, IMU, CAMERA, BATTERY, CPU }

private val HudText = Color(0xFFE6EEF5)
private val HudBlue = Color(0xFF00E5FF)
private val HudBlueD = Color(0xFF16589f)
private val HudBorder = Color(0xFF27719A)
private val HudGlow = Color(0xFF4DB9DB)
private val HudRed = Color(0xFFFF3D00)
private val Green = Color(0xFF00FF00)


@Composable
fun JaxHudScreen(
    modifier: Modifier = Modifier,
    robotName: String = "Jax-1",
    batteryPercent: Int = 0,
    cpuTemp: Int = 0,
    isLinked: Boolean = false,
    motorsActive: Boolean = false,
    imuActive: Boolean = false,
    cameraActive: Boolean = false,
    leftJoystickValue: Pair<Float, Float> = 0f to 0f,
    rightJoystickValue: Pair<Float, Float> = 0f to 0f,
    selectedMode: String = "walk",
    modes: List<RobotMode> = listOf(
        RobotMode("STAND", "stand"),
        RobotMode("WALK", "walk"),
        RobotMode("SIT", "sit"),
        RobotMode("LAY", "lay")
    ),
    enabledIndicators: Set<HudIndicator> = HudIndicator.values().toSet(),
    videoActive: Boolean = false,
    hapticsEnabled: Boolean = true,
    onVideoToggle: (Boolean) -> Unit = {},
    onLeftJoystickChanged: (x: Float, y: Float) -> Unit = { _, _ -> },
    onRightJoystickChanged: (x: Float, y: Float) -> Unit = { _, _ -> },
    onModeSelected: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onTerminateClick: () -> Unit = {},
    videoFeed: @Composable () -> Unit = { CameraPlaceholder(modifier = Modifier.fillMaxSize()) }
) {
    var mode by remember(selectedMode, modes) {
        mutableStateOf(
            modes.firstOrNull { it.command.equals(selectedMode, ignoreCase = true) }
                ?: modes.firstOrNull()
                ?: RobotMode("WALK", "walk")
        )
    }

    // Filter LEDs based on user choice
    val activeLedList = remember(enabledIndicators, isLinked, motorsActive, imuActive, cameraActive) {
        mutableListOf<Triple<String, Color, Boolean>>().apply {
            if (enabledIndicators.contains(HudIndicator.ROS_LINK))
                add(Triple("ROS LINK", if (isLinked) Green else HudBlueD, isLinked))
            if (enabledIndicators.contains(HudIndicator.MOTORS))
                add(Triple("MOTORS", if (motorsActive) Green else HudBlueD, motorsActive))
            if (enabledIndicators.contains(HudIndicator.IMU))
                add(Triple("IMU", if (imuActive) Green else HudBlueD, imuActive))
            if (enabledIndicators.contains(HudIndicator.CAMERA))
                add(Triple("CAMERA", if (cameraActive) Green else HudBlueD, cameraActive))
        }
    }

    val haptic = LocalHapticFeedback.current

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.hud_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, top = 8.dp, end = 22.dp, bottom = 0.dp)
        ) {
            HudTopBar(robotName = robotName, batteryPercent = batteryPercent, isLinked = isLinked)

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LEFT PANEL (Status Indicators)
                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        StatusGroup(
                            items = activeLedList,
                            alignEnd = false
                        )
                    }

                    HudJoystick(
                        value = leftJoystickValue,
                        onValueChanged = onLeftJoystickChanged,
                        hapticsEnabled = hapticsEnabled,
                        modifier = Modifier
                            .size(180.dp)
                            .align(Alignment.BottomCenter)
                    )
                }

                // CENTER PANEL (Video Feed)
                Column(
                    modifier = Modifier
                        .weight(0.66f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.9f)
                            .border(
                                1.dp,
                                HudBlueD.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color.Black
                    ) {
                        videoFeed()
                    }
                }

                // RIGHT PANEL (Telemetry & Battery)
                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Filtered Telemetry List
                        val telemetryItems = mutableListOf<Triple<String, Color, Boolean>>().apply {
                            if (enabledIndicators.contains(HudIndicator.BATTERY)) {
                                add(Triple("BAT $batteryPercent%", if (!isLinked) HudBlueD else if (batteryPercent < 20) HudRed else Green, isLinked))
                            }
                            if (enabledIndicators.contains(HudIndicator.CPU)) {
                                add(Triple("CPU $cpuTemp°", if (!isLinked) HudBlueD else if (cpuTemp > 75) HudRed else Green, isLinked))
                            }
                        }

                        StatusGroup(
                            items = telemetryItems,
                            alignEnd = true
                        )
                    }

                    HudJoystick(
                        value = rightJoystickValue,
                        onValueChanged = onRightJoystickChanged,
                        hapticsEnabled = hapticsEnabled,
                        modifier = Modifier
                            .size(180.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                modes.forEach { robotMode ->
                    DynamicModeButton(
                        mode = robotMode,
                        selected = mode.command.equals(robotMode.command, ignoreCase = true),
                        onClick = {
                            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            mode = robotMode
                            onModeSelected(robotMode.command)
                        }
                    )
                    Spacer(Modifier.width(6.dp))
                }

                ModeButton(
                    selected = videoActive,
                    icon = {
                        Icon(
                            Icons.Outlined.Videocam,
                            contentDescription = "VIDEO",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.size(height = 52.dp, width = 72.dp),
                    onClick = {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onVideoToggle(!videoActive)
                    }
                )
            }
        }

        // TERMINATE/POWER BUTTON (LEFT)
        IconButton(
            onClick = onTerminateClick,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 10.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Terminate Session",
                tint = HudRed.copy(alpha = 0.8f),
                modifier = Modifier.size(32.dp)
            )
        }

        // SETTINGS BUTTON (RIGHT)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 10.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun DynamicModeButton(
    mode: RobotMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val knownIcon = knownModeIcon(mode.command)

    if (knownIcon != null) {
        ModeButton(
            selected = selected,
            icon = {
                Image(
                    painter = painterResource(id = knownIcon),
                    contentDescription = mode.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            },
            modifier = Modifier.size(height = 52.dp, width = 64.dp),
            onClick = onClick
        )
    } else {
        TextModeButton(
            label = mode.label,
            selected = selected,
            onClick = onClick
        )
    }
}

private fun knownModeIcon(command: String): Int? {
    return when (command.lowercase()) {
        "stand" -> R.drawable.jax_stand_final
        "walk" -> R.drawable.jax_walk_final
        "sit" -> R.drawable.jax_sit_final
        "lay" -> R.drawable.jax_lay_final
        "shake" -> R.drawable.jax_shake_final
        "wave" -> R.drawable.jax_wave_final
        else -> null
    }
}

@Composable
private fun HudTopBar(robotName: String, batteryPercent: Int, isLinked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = robotName.uppercase(),
            color = HudText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.weight(1f))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.weight(1f))

        Text(
            text = if (isLinked) "ROS LINK: ONLINE" else "ROS LINK: OFFLINE",
            color = HudText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.Bottom)
                .padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right-side Topbar empty for balance
        }
    }
}

@Composable
private fun StatusGroup(
    items: List<Triple<String, Color, Boolean>>,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { (label, color, pulse) ->
            StatusRow(label = label, color = color, pulse = pulse, alignEnd = alignEnd)
        }
    }
}

@Composable
private fun StatusRow(label: String, color: Color, pulse: Boolean, alignEnd: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(14.dp)
    ) {
        if (alignEnd) {
            Text(
                text = label,
                color = HudText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                lineHeight = 10.sp
            )
            Spacer(Modifier.width(6.dp))
            PulsingDot(color = color, animate = pulse)
        } else {
            PulsingDot(color = color, animate = pulse)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = HudText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                lineHeight = 10.sp
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color, animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "dot")
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (animate) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    Box(
        modifier = Modifier.size(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        )
    }
}

@Composable
private fun ModeButton(
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = if (selected) HudBlueD.copy(alpha = 0.5f) else Color(0x44000000),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    if (selected) HudBlueD else HudBlueD.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@Composable
private fun TextModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(52.dp)
            .width(90.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = if (selected) HudBlueD.copy(alpha = 0.3f) else Color(0x44000000),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    if (selected) HudBlueD else HudBlueD.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun CameraPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color.Black))
}

@Composable
fun HudJoystick(
    value: Pair<Float, Float>,
    onValueChanged: (x: Float, y: Float) -> Unit,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember { mutableStateOf(Offset(value.first, value.second)) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) * 0.45f
                        val delta = start - center
                        val dist = sqrt(delta.x * delta.x + delta.y * delta.y)

                        if (hapticsEnabled && dist >= radius) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        val clamped = if (dist > radius && dist != 0f) delta * (radius / dist) else delta
                        val normalized = Offset(clamped.x / radius, clamped.y / radius)
                        dragOffset = normalized
                        onValueChanged(normalized.x, -normalized.y)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) * 0.45f
                        val delta = change.position - center
                        val dist = sqrt(delta.x * delta.x + delta.y * delta.y)

                        if (hapticsEnabled && dist >= radius) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        val clamped = if (dist > radius && dist != 0f) delta * (radius / dist) else delta
                        val normalized = Offset(clamped.x / radius, clamped.y / radius)
                        dragOffset = normalized
                        onValueChanged(normalized.x, -normalized.y)
                    },
                    onDragEnd = {
                        dragOffset = Offset.Zero
                        onValueChanged(0f, 0f)
                    },
                    onDragCancel = {
                        dragOffset = Offset.Zero
                        onValueChanged(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.width * 0.45f
            val innerR = outerR * 0.35f

            drawCircle(
                color = HudBlueD.copy(alpha = 0.9f),
                radius = outerR,
                center = c,
                style = Stroke(width = 3.dp.toPx())
            )

            drawCircle(
                color = Black.copy(alpha = 0.5f),
                radius = outerR,
                center = c
            )

            rotate(45f, c) {
                drawLine(
                    color = HudBlueD.copy(alpha = 0.9f),
                    start = Offset(c.x - outerR, c.y),
                    end = Offset(c.x + outerR, c.y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = HudBlueD.copy(alpha = 0.9f),
                    start = Offset(c.x, c.y - outerR),
                    end = Offset(c.x, c.y + outerR),
                    strokeWidth = 2.dp.toPx()
                )
            }

            drawCircle(
                color = Color(0xFF0B141D),
                radius = innerR,
                center = c
            )
            drawCircle(
                color = HudBlueD.copy(alpha = 0.8f),
                radius = innerR,
                center = c,
                style = Stroke(width = 2.dp.toPx())
            )

            val knobCenter = Offset(
                c.x + dragOffset.x * (outerR * 0.8f),
                c.y + dragOffset.y * (outerR * 0.8f)
            )
            drawCircle(
                color = HudBlueD.copy(alpha = 0.9f),
                radius = 15.dp.toPx(),
                center = knobCenter
            )
            drawCircle(
                color = HudBlueD.copy(alpha = 0.3f),
                radius = 10.dp.toPx(),
                center = knobCenter,
                style = Stroke(width = 30.dp.toPx())
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = HudText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = HudText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = null,
            tint = HudText,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = HudText,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .size(20.dp)
        )
    }
}

@Preview(name = "Robot Offline", widthDp = 891, heightDp = 411, showBackground = true)
@Composable
fun JaxHudScreenPreviewOffline() {
    JaxHudScreen(
        robotName = "OFFLINE",
        isLinked = false,
        motorsActive = false,
        batteryPercent = 0,
        cpuTemp = 0,
        videoActive = false,
        modes = listOf(RobotMode("STAND", "stand"), RobotMode("WALK", "walk"))
    )
}

@Preview(name = "Robot Online", widthDp = 891, heightDp = 411, showBackground = true)
@Composable
fun JaxHudScreenPreviewOnline() {
    JaxHudScreen(
        robotName = "ApeX-1",
        isLinked = true,
        motorsActive = true,
        imuActive = true,
        cameraActive = true,
        batteryPercent = 88,
        cpuTemp = 42,
        videoActive = true,
        videoFeed = {
            Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                Text("CAMERA FEED ACTIVE", color = Color.White, fontSize = 12.sp)
            }
        }
    )
}