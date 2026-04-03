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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaxgamepad.R
import com.example.jaxgamepad.RobotMode
import kotlin.math.min
import kotlin.math.sqrt

private val HudText = Color(0xFFE6EEF5)
private val HudBlue = Color(0xFF00E5FF)
private val HudBorder = Color(0xFF1A3A4A)

@Composable
fun JaxHudScreen(
    modifier: Modifier = Modifier,
    robotName: String = "Jax-1",
    batteryPercent: Int = 78,
    cameraActive: Boolean = true,
    leftJoystickValue: Pair<Float, Float> = 0f to 0f,
    rightJoystickValue: Pair<Float, Float> = 0f to 0f,
    selectedMode: String = "walk",
    modes: List<RobotMode> = listOf(
        RobotMode("STAND", "stand"),
        RobotMode("WALK", "walk"),
        RobotMode("SIT", "sit"),
        RobotMode("LAY", "lay")
    ),
    videoActive: Boolean = false,
    onVideoToggle: (Boolean) -> Unit = {},
    onLeftJoystickChanged: (x: Float, y: Float) -> Unit = { _, _ -> },
    onRightJoystickChanged: (x: Float, y: Float) -> Unit = { _, _ -> },
    onModeSelected: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    videoFeed: @Composable () -> Unit = { CameraPlaceholder(modifier = Modifier.fillMaxSize()) }
) {
    var mode by remember(selectedMode, modes) {
        mutableStateOf(
            modes.firstOrNull { it.command.equals(selectedMode, ignoreCase = true) }
                ?: modes.firstOrNull()
                ?: RobotMode("WALK", "walk")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B10))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, top = 8.dp, end = 22.dp, bottom = 4.dp)
        ) {
            HudTopBar(robotName = robotName, batteryPercent = batteryPercent)

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                thickness = 1.dp,
                color = HudBorder.copy(alpha = 0.3f)
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                ) {
                    StatusGroup(
                        items = listOf(
                            Triple("LINK", HudBlue, true),
                            Triple("MOTORS", HudBlue, true),
                            Triple("IMU", HudBlue, true),
                            Triple("SAFE", HudBlue, true),
                        ),
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    HudJoystick(
                        value = leftJoystickValue,
                        onValueChanged = onLeftJoystickChanged,
                        modifier = Modifier
                            .size(180.dp)
                            .align(Alignment.BottomCenter)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.77f)
                            .border(1.dp, HudBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        color = Color.Black
                    ) {
                        videoFeed()
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                ) {
                    StatusGroup(
                        items = listOf(
                            Triple(if (cameraActive) "CAM ON" else "CAM OFF", HudBlue, true),
                            Triple("FPS 30", HudBlue, true),
                        ),
                        alignEnd = true,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )

                    HudJoystick(
                        value = rightJoystickValue,
                        onValueChanged = onRightJoystickChanged,
                        modifier = Modifier
                            .size(180.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                modes.forEach { robotMode ->
                    DynamicModeButton(
                        mode = robotMode,
                        selected = mode.command.equals(robotMode.command, ignoreCase = true),
                        onClick = {
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
                        onVideoToggle(!videoActive)
                    }
                )
            }
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.5f),
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
private fun HudTopBar(robotName: String, batteryPercent: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = robotName,
            color = HudText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = "ROS2 LINK: LIVE  |  CTRL: READY",
            color = HudText,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "BAT $batteryPercent%",
                color = HudText,
                fontSize = 13.sp
            )
            Spacer(Modifier.width(6.dp))
            BatteryGlyph(level = batteryPercent / 100f)
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
            StatusRow(label = label, color = color, pulse = pulse)
        }
    }
}

@Composable
private fun StatusRow(label: String, color: Color, pulse: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PulsingDot(color = color, animate = pulse)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = HudText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
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
                .size(8.dp)
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
private fun BatteryGlyph(level: Float) {
    Box(
        modifier = Modifier
            .width(22.dp)
            .height(12.dp)
            .border(1.dp, HudText.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            .padding(1.5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(level.coerceIn(0f, 1f))
                .background(HudBlue)
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
        color = if (selected) HudBlue.copy(alpha = 0.3f) else Color(0x44000000),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    if (selected) HudBlue else HudBorder.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
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
        color = if (selected) HudBlue.copy(alpha = 0.3f) else Color(0x44000000),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    1.dp,
                    if (selected) HudBlue else HudBorder.copy(alpha = 0.4f),
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
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember { mutableStateOf(Offset(value.first, value.second)) }

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
                color = HudBorder.copy(alpha = 0.4f),
                radius = outerR,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )

            drawCircle(
                color = Color(0x1A5D7388),
                radius = outerR,
                center = c
            )

            rotate(45f, c) {
                drawLine(
                    color = HudBorder.copy(alpha = 0.3f),
                    start = Offset(c.x - outerR, c.y),
                    end = Offset(c.x + outerR, c.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = HudBorder.copy(alpha = 0.3f),
                    start = Offset(c.x, c.y - outerR),
                    end = Offset(c.x, c.y + outerR),
                    strokeWidth = 1.dp.toPx()
                )
            }

            drawCircle(
                color = Color(0xFF0B141D),
                radius = innerR,
                center = c
            )
            drawCircle(
                color = HudBorder.copy(alpha = 0.8f),
                radius = innerR,
                center = c,
                style = Stroke(width = 2.dp.toPx())
            )

            val knobCenter = Offset(
                c.x + dragOffset.x * (outerR * 0.8f),
                c.y + dragOffset.y * (outerR * 0.8f)
            )
            drawCircle(
                color = HudBlue.copy(alpha = 0.9f),
                radius = 6.dp.toPx(),
                center = knobCenter
            )
            drawCircle(
                color = HudBlue.copy(alpha = 0.5f),
                radius = 10.dp.toPx(),
                center = knobCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = HudText.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = HudText.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = null,
            tint = HudText.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = HudText.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .size(20.dp)
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720, showBackground = true)
@Composable
private fun JaxHudScreenPreview() {
    JaxHudScreen(
        modes = listOf(
            RobotMode("STAND", "stand"),
            RobotMode("WALK", "walk"),
            RobotMode("SIT", "sit"),
            RobotMode("LAY", "lay"),
            RobotMode("DANCE", "dance")
        ),
        videoActive = true
    )
}
