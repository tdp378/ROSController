package com.jax.roscontroller.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jax.roscontroller.R
import com.jax.roscontroller.RobotMode
import kotlin.math.min
import kotlin.math.sqrt
import com.jax.roscontroller.ui.theme.MyColors

// Enum for LED and Telemetry selection
enum class HudIndicator { ROS_LINK, ODOM, IMU, CAMERA, BATTERY, CPU, FOOT_SENSORS }



private val HudBorder = Color(0xFF27719A)
private val HudGlow = Color(0xFF4DB9DB)
private val HudRed = Color(0xFFFF3D00)
private val Green = Color(0xFF00FF00)

@Composable
fun JaxHudScreen(
    modifier: Modifier = Modifier,
    robotName: String = "Jax-1",
    sessionTimeText: String = "00:00:00",
    batteryPercent: Int = 0,
    batteryVoltage: Double? = null,
    cpuTemp: Int = 0,
    isLinked: Boolean = false,
    odomActive: Boolean = false,
    totalDistance: Double = 0.0,
    imuActive: Boolean = false,
    cameraActive: Boolean = false,
    footSensorsActive: Boolean = false,
    batteryActive: Boolean = isLinked,
    cpuActive: Boolean = isLinked,
    leftJoystickValue: Pair<Float, Float> = 0f to 0f,
    rightJoystickValue: Pair<Float, Float> = 0f to 0f,
    heightSliderValue: Float = 0f,
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
    onHeightSliderChanged: (Float) -> Unit = {},
    onModeSelected: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onTerminateClick: () -> Unit = {},
    videoFeed: @Composable () -> Unit = { CameraPlaceholder(modifier = Modifier.fillMaxSize()) }
)
 {
    var mode by remember(selectedMode, modes) {
        mutableStateOf(
            modes.firstOrNull { it.command.equals(selectedMode, ignoreCase = true) }
                ?: modes.firstOrNull()
                ?: RobotMode("WALK", "walk")
        )
    }


    var speedSliderValue by remember { mutableStateOf(0.0f) }

    val activeLedList = remember(enabledIndicators, isLinked, imuActive, cameraActive, footSensorsActive) {
        mutableListOf<Triple<String, Color, Boolean>>().apply {
            if (enabledIndicators.contains(HudIndicator.ROS_LINK))
                add(Triple("ROS LINK", if (isLinked) Green else MyColors.HudBlueD, isLinked))
            if (enabledIndicators.contains(HudIndicator.FOOT_SENSORS))
                add(Triple("FOOT SENSORS", if (footSensorsActive) Green else MyColors.HudBlueD, footSensorsActive))
            if (enabledIndicators.contains(HudIndicator.IMU))
                add(Triple("IMU", if (imuActive) Green else MyColors.HudBlueD, imuActive))
            if (enabledIndicators.contains(HudIndicator.CAMERA))
                add(Triple("CAMERA", if (cameraActive) Green else MyColors.HudBlueD, cameraActive))
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
            HudTopBar(
                robotName = robotName,
                sessionTimeText = sessionTimeText,
                batteryPercent = batteryPercent,
                isLinked = isLinked
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LEFT PANEL
                Box(
                    modifier = Modifier
                        .weight(0.20f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        StatusGroup(items = activeLedList, alignEnd = false)
                    }

                    HudJoystick(
                        value = leftJoystickValue,
                        onValueChanged = onLeftJoystickChanged,
                        hapticsEnabled = hapticsEnabled,
                        modifier = Modifier
                            .size(170.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-10).dp)
                            .offset(y = (22).dp)
                    )

                    HudVerticalSlider(
                        value = heightSliderValue,
                        onValueChange = onHeightSliderChanged,
                        label = "HGT",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxHeight()
                            .width(40.dp)
                            .padding(bottom = 20.dp)
                    )
                }

                // CENTER PANEL
                Column(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.777f)
                            .border(1.dp, MyColors.HudBlueD.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color.Black
                    ) {
                        videoFeed()
                    }
                }

                // RIGHT PANEL
                Box(
                    modifier = Modifier
                        .weight(0.20f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        val telemetryItems = mutableListOf<Triple<String, Color, Boolean>>().apply {
                            if (enabledIndicators.contains(HudIndicator.BATTERY)) {
                                val isLow = if (batteryPercent > 0) {
                                    batteryPercent < 20
                                } else if (batteryVoltage != null && batteryVoltage > 0.0) {
                                    // Threshold for 4S system (16.9V full -> 14.0V low)
                                    batteryVoltage < 14.0
                                } else {
                                    false
                                }

                                val batteryColor = when {
                                    !batteryActive -> MyColors.HudBlueD
                                    isLow -> HudRed
                                    else -> Green
                                }
                                
                                val batteryLabel = if (batteryPercent > 0) {
                                    "BATTERY $batteryPercent%"
                                } else if (batteryVoltage != null && batteryVoltage > 0.0) {
                                    String.format("BATTERY %.2fV", batteryVoltage)
                                } else {
                                    "BATTERY 0%"
                                }

                                add(Triple(batteryLabel, batteryColor, batteryActive))
                            }
                            if (enabledIndicators.contains(HudIndicator.CPU)) {
                                val cpuColor = when {
                                    !cpuActive -> MyColors.HudBlueD
                                    cpuTemp > 75 -> HudRed
                                    else -> Green
                                }
                                add(Triple("CPU TEMP $cpuTemp°", cpuColor, cpuActive))
                            }
                            if (enabledIndicators.contains(HudIndicator.ODOM)) {
                                val distStr = String.format("%.2fM", totalDistance)
                                add(Triple("ODOM $distStr", if (odomActive) Green else MyColors.HudBlueD, odomActive))
                            }
                        }
                        StatusGroup(items = telemetryItems, alignEnd = true, modifier = Modifier.padding(end = 4.dp))
                    }

                    HudJoystick(
                        value = rightJoystickValue,
                        onValueChanged = onRightJoystickChanged,
                        hapticsEnabled = hapticsEnabled,
                        modifier = Modifier
                            .size(170.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp)
                            .offset(y = (22).dp)
                    )

                    HudVerticalSlider(
                        value = speedSliderValue,
                        onValueChange = { speedSliderValue = it },
                        label = "SPD",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxHeight()
                            .width(40.dp)
                            .padding(bottom = 20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                modes.forEach { robotMode ->
                    DynamicModeButton(
                        mode = robotMode,
                        selected = mode.label.equals(robotMode.label, ignoreCase = true),
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

        IconButton(
            onClick = onTerminateClick,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 10.dp)
                .size(48.dp)
        ) {
            Icon(
                Icons.Default.PowerSettingsNew,
                "Terminate",
                tint = HudRed.copy(alpha = 0.8f),
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 10.dp)
                .size(48.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                "Settings",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HudVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val animatableValue = remember { Animatable(value) }

    // Synchronize animatable value with external value changes (except when animating)
    LaunchedEffect(value) {
        if (!animatableValue.isRunning) {
            animatableValue.snapTo(value)
        }
    }

    val clampedValue = animatableValue.value.coerceIn(-1f, 1f)
    val displayValue01 = (clampedValue + 1f) / 2f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (enabled) MyColors.HudText else MyColors.HudText.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                animatableValue.animateTo(0f, tween(300))
                            }
                            // We still want to notify the parent of the target value immediately or as it animates
                            // For a smooth "return to zero" feel, we'll update the parent as it goes
                        }
                    )
                }
            }
        )

        // Add a side effect to notify parent of animation progress
        LaunchedEffect(animatableValue.value) {
            if (animatableValue.isRunning) {
                onValueChange(animatableValue.value)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val trackHeightPx = with(density) { maxHeight.toPx() }

            val thumbHeight = 28.dp
            val thumbWidth = 28.dp
            val railWidth = 14.dp

            val thumbHeightPx = with(density) { thumbHeight.toPx() }
            val usableHeightPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
            val thumbOffsetPx = (1f - displayValue01) * usableHeightPx
            val thumbOffsetDp = with(density) { thumbOffsetPx.toDp() }

            fun positionToValue(y: Float): Float {
                val raw01 = 1f - ((y - thumbHeightPx / 2f) / usableHeightPx)
                val clamped01 = raw01.coerceIn(0f, 1f)
                return (clamped01 * 2f) - 1f
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(enabled) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                if (enabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch {
                                        animatableValue.snapTo(positionToValue(offset.y))
                                        onValueChange(animatableValue.value)
                                    }
                                }
                            },
                            onVerticalDrag = { change, _ ->
                                if (enabled) {
                                    scope.launch {
                                        animatableValue.snapTo(positionToValue(change.position.y))
                                        onValueChange(animatableValue.value)
                                    }
                                    change.consumePositionChange()
                                }
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .width(railWidth + 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(
                            1.dp,
                            MyColors.HudBlueD.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp)
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .width(railWidth)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MyColors.HudBlueD.copy(alpha = 0.18f))
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(railWidth)
                        .fillMaxHeight(displayValue01)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MyColors.HudBlueD.copy(alpha = 0.20f),
                                    MyColors.HudBlueD.copy(alpha = 0.55f),
                                    MyColors.HudBlueD.copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = thumbOffsetDp)
                        .size(width = thumbWidth + 8.dp, height = thumbHeight + 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MyColors.HudBlueD.copy(alpha = 0.15f))
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = thumbOffsetDp)
                        .size(width = thumbWidth, height = thumbHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1C3E5A),
                                    MyColors.HudBlueD,
                                    Color(0xFF0E6FB3)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            MyColors.HudText.copy(alpha = 0.35f),
                            RoundedCornerShape(6.dp)
                        )
                )

                Text(
                    text = String.format("%.2f", clampedValue),
                    color = MyColors.HudText.copy(alpha = 0.85f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .pointerInput(enabled) {
                            if (enabled) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            animatableValue.animateTo(0f, tween(300))
                                        }
                                    }
                                )
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun DynamicModeButton(mode: RobotMode, selected: Boolean, onClick: () -> Unit) {
    // CHANGED: passing mode.label here
    val iconRes = knownModeIcon(mode.label)

    if (iconRes != null) {
        ModeButton(
            selected = selected,
            icon = {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = mode.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            },
            modifier = Modifier.size(height = 52.dp, width = 64.dp),
            onClick = onClick
        )
    } else {
        // Fallback to text if the label doesn't match our "Known" list
        TextModeButton(label = mode.label, selected = selected, onClick = onClick)
    }
}

private fun knownModeIcon(label: String): Int? {
    // We use trim() to catch accidental spaces and lowercase() for case-insensitivity
    return when (label.trim().lowercase()) {
        "stand" -> R.drawable.jax_stand_final
        "walk"  -> R.drawable.jax_walk_final
        "sit"   -> R.drawable.jax_sit_final
        "lay"   -> R.drawable.jax_lay_final
        "shake" -> R.drawable.jax_shake_final
        "wave"  -> R.drawable.jax_wave_final
        else    -> null
    }
}

@Composable
private fun HudTopBar(
    robotName: String,
    sessionTimeText: String,
    batteryPercent: Int,
    isLinked: Boolean
) {
    // ROBOT NAME (top row)
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1f))
        Text(
            robotName.uppercase(),
            color = MyColors.HudText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.weight(1f))
    }

    // SECOND ROW (session + link)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // LEFT: SESSION TIMER
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = "SESSION: $sessionTimeText",
                color = MyColors.HudText.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // CENTER: ROS STATUS
        Text(
            text = if (isLinked) "ROS LINK: ONLINE" else "ROS LINK: OFFLINE",
            color = if (isLinked) MyColors.HudText else Color(0xFFFF3B30),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(vertical = 8.dp)

        )

        // RIGHT SPACER
        Box(modifier = Modifier.weight(1f))
    }
}
@Composable
private fun StatusGroup(
    items: List<Triple<String, Color, Boolean>>,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
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
                color = MyColors.HudText,
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
                color = MyColors.HudText,
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
    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = if (selected) MyColors.HudBlueD.copy(alpha = 0.5f) else Color(0x44000000),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    if (selected) MyColors.HudBlueD else MyColors.HudBlueD.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) { icon() }
    }
}

@Composable
private fun TextModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(52.dp)
            .width(90.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = if (selected) MyColors.HudBlueD.copy(alpha = 0.3f) else Color(0x44000000),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    if (selected) MyColors.HudBlueD else MyColors.HudBlueD.copy(alpha = 0.4f),
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
                        val clamped =
                            if (dist > radius && dist != 0f) delta * (radius / dist) else delta
                        dragOffset = Offset(clamped.x / radius, clamped.y / radius)
                        onValueChanged(dragOffset.x, -dragOffset.y)
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
                        val clamped =
                            if (dist > radius && dist != 0f) delta * (radius / dist) else delta
                        dragOffset = Offset(clamped.x / radius, clamped.y / radius)
                        onValueChanged(dragOffset.x, -dragOffset.y)
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
            val outerR = size.width * 0.38f
            val innerR = outerR * 0.28f
            drawCircle(color = MyColors.HudBlueD.copy(alpha = 0.9f), radius = outerR, center = c, style = Stroke(width = 3.dp.toPx()))
            drawCircle(color = MyColors.Black.copy(alpha = 0.5f), radius = outerR, center = c)
            rotate(45f, c) {
                drawLine(
                    color = MyColors.HudBlueD.copy(alpha = 0.9f),
                    start = Offset(c.x - outerR, c.y),
                    end = Offset(c.x + outerR, c.y),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = MyColors.HudBlueD.copy(alpha = 0.9f),
                    start = Offset(c.x, c.y - outerR),
                    end = Offset(c.x, c.y + outerR),
                    strokeWidth = 2.dp.toPx()
                )
            }
            drawCircle(color = Color(0xFF0B141D), radius = innerR, center = c)
            drawCircle(color = MyColors.HudBlueD.copy(alpha = 0.8f), radius = innerR, center = c, style = Stroke(width = 2.dp.toPx()))
            val knobCenter = Offset(c.x + dragOffset.x * (outerR * 0.8f), c.y + dragOffset.y * (outerR * 0.8f))
            drawCircle(color = MyColors.HudBlueD.copy(alpha = 0.9f), radius = 10.dp.toPx(), center = knobCenter)
            drawCircle(color = MyColors.HudBlueD.copy(alpha = 0.3f), radius = 5.dp.toPx(), center = knobCenter, style = Stroke(width = 30.dp.toPx()))
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = MyColors.HudText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MyColors.HudText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = null,
            tint = MyColors.HudText,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 30.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MyColors.HudText,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 30.dp)
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
        odomActive = false,
        batteryPercent = 0,
        cpuTemp = 0,
        videoActive = false,
        modes = listOf(
            RobotMode("STAND", "stand"),
            RobotMode("WALK", "walk")
        )
    )
}

@Preview(name = "Robot Online", widthDp = 891, heightDp = 411, showBackground = true)
@Composable
fun JaxHudScreenPreviewOnline() {
    JaxHudScreen(
        robotName = "ApeX-1",
        isLinked = true,
        odomActive = true,
        imuActive = true,
        cameraActive = true,
        batteryPercent = 88,
        cpuTemp = 42,
        videoActive = true,
        videoFeed = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text("CAMERA FEED ACTIVE", color = Color.White, fontSize = 12.sp)
            }
        }
    )
}
