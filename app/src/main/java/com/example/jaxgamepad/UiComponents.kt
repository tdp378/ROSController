package com.example.jaxgamepad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.jaxgamepad.ui.HudIndicator
import com.example.jaxgamepad.ui.theme.MyColors


@Composable
fun CyberDialog(
    show: Boolean,
    title: String,
    confirmText: String = "LAUNCH ▶",
    useTerminalLook: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!show) return

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = MyColors.HudBlue, // Explicitly named to avoid type mismatch
                    spotColor = MyColors.HudBlue    // Explicitly named
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(MyColors.HudBlue, Color(0xFF008CFF), MyColors.HudBlue)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF020617).copy(alpha = 0.95f),
                            Color(0xFF020617).copy(alpha = 0.88f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(10.dp)
        ) {
            // ... rest of dialog content remains the same
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 700.dp)
            ) {
                Text(
                    text = title,
                    color = MyColors.HudText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (useTerminalLook) {
                        CyberTerminalBox { content() }
                    } else {
                        content()
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("DISMISS", color = MyColors.HudText.copy(alpha = 0.75f))
                    }

                    if (confirmText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        CyberButton(
                            onClick = onConfirm,
                            modifier = Modifier.height(35.dp).width(130.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, MyColors.HudBlue, RoundedCornerShape(10.dp))
                                    .background(MyColors.HudBlue.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(confirmText, color = MyColors.HudBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CyberButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun CyberTerminalBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1.8f)) {
        Image(
            painter = painterResource(R.drawable.terminal_box),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 54.dp, top = 26.dp, end = 50.dp, bottom = 22.dp),
            content = content
        )
    }
}


@Composable
fun HelpSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = MyColors.HudBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
fun HelpBullet(text: String) {
    Text(
        text = "• $text",
        color = MyColors.HudText,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}

@Composable
fun HelpNote(text: String) {
    Text(
        text = text,
        color = MyColors.HudText,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        textDecoration = TextDecoration.Underline


    )
}


@Composable
fun HelpDialog(
    show: Boolean,
    onDismiss: () -> Unit
) {
    CyberDialog(
        show = show,
        title = "SYSTEM HELP",
        confirmText = "",
        onConfirm = {},
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {


            HelpSection("MINIMUM REQUIREMENTS") {
                HelpBullet("rosbridge websocket (ws://<ip>:9090)")
                HelpBullet("geometry_msgs/Twist (motion)")
                HelpBullet("std_msgs/String (mode)")
            }

            HelpSection("ADD NEW ROBOT") {
                HelpNote("Robot Tab")

                HelpBullet("Enter robot name")
                HelpBullet("Enter rosbridge IP:port (192.168.x.x:9090)")
                HelpBullet("Enter video stream URL (optional)")
                HelpBullet("Upload robot image (optional)")
                HelpBullet("Axis settings should stay normal unless the controller is reversed")
                HelpNote("Topics Tab")
                HelpBullet("Launch rosbridge on robot")
                HelpBullet("Tap auto discover topics")
                HelpBullet("Use dropdowns to verify topics assigned")
                HelpBullet("Most important is cmd_vel and mode topic")
                HelpNote("Modes Tab")
                HelpBullet("Verify configured modes match what the robot expects")
                HelpBullet("Add or remove modes as needed")
                HelpBullet("Save robot configuration")
            }

            HelpSection("CONTROLS") {
                HelpBullet("Left stick -> movement")
                HelpBullet("Right stick -> turn/body")
                HelpBullet("Left slider -> height")
                HelpBullet("Right slider -> speed")
            }


            HelpSection("TROUBLESHOOTING") {
                HelpBullet("No movement -> check cmd_vel topic. Your ROS node must be configured to use geometry_msgs/Twist ")
                HelpBullet("Modes fail -> check mode topic. Your ROS node must be configured to use std_msgs/String")
                HelpBullet("No topics listed when taping auto discover topics -> check that rosbridge is running and ensure network is connected")
                HelpBullet("Wrong motion -> axis mismatch. Choose inverted on the incorrect axis in setup")
            }


        }
    }
}

@Composable
fun SettingsRockerRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        onClick = { onToggle(!checked) },
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp)),
        color = Color.Black.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MyColors.HudBlue.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (checked) MyColors.HudText else Color.Gray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Switch(
                checked = checked,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MyColors.HudBlue,
                    checkedTrackColor = MyColors.HudBlue.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.DarkGray,
                    uncheckedTrackColor = Color.Black.copy(alpha = 0.4f),
                    uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.scale(0.6f)
            )
        }
    }
}

@Composable
fun IndicatorRockerRow(
    indicator: HudIndicator,
    activeIndicators: Set<HudIndicator>,
    onToggle: (HudIndicator) -> Unit
) {
    val isEnabled = activeIndicators.contains(indicator)

    Surface(
        onClick = { onToggle(indicator) },
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp)),
        color = Color.Black.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MyColors.HudBlue.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = indicator.name.replace("_", " "),
                color = if (isEnabled) MyColors.HudText else Color.Gray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Switch(
                checked = isEnabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MyColors.HudBlue,
                    checkedTrackColor = MyColors.HudBlue.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.DarkGray,
                    uncheckedTrackColor = Color.Black.copy(alpha = 0.4f),
                    uncheckedBorderColor = MyColors.HudText.copy(alpha = 0.2f),
                    checkedBorderColor = MyColors.HudText.copy(alpha = 0.2f)
                ),
                modifier = Modifier.scale(0.6f)
            )
        }
    }
}

@Composable
fun AccountStatusDialog(
    show: Boolean,
    user: com.google.firebase.auth.FirebaseUser?,
    onDismiss: () -> Unit,
    onGoogleLogin: () -> Unit,
    onSignOut: () -> Unit
) {
    if (!show) return

    var showLoginForm by remember { mutableStateOf(false) }
    var isLoginMode by remember { mutableStateOf(true) }
    var submitTrigger by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    val isSignedIn = user != null
    val title = if (showLoginForm) "SYSTEM_AUTHENTICATION" else if (isSignedIn) "AUTH_SUCCESSFUL" else "GUEST_ACCESS"

    val confirmText = if (showLoginForm && !isSignedIn) {
        if (isLoading) "PROCESSING..." else if (isLoginMode) "LOGIN ▶" else "CREATE ▶"
    } else ""

    CyberDialog(
        show = show,
        title = title,
        confirmText = confirmText,
        onConfirm = {
            if (showLoginForm && !isSignedIn && !isLoading) {
                submitTrigger++
            }
        },
        onDismiss = {
            showLoginForm = false
            onDismiss()
        }
    ) {
        if (showLoginForm && !isSignedIn) {
            ProfileContent(
                onBack = { showLoginForm = false },
                isLoginMode = isLoginMode,
                onModeChange = { isLoginMode = it },
                submitTrigger = submitTrigger,
                onLoadingChange = { isLoading = it },
                onProfileCreated = {
                    showLoginForm = false
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Info Section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MyColors.HudBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isSignedIn) "IDENTIFIED: ${user?.email}" else "STATUS: UNREGISTERED",
                        color = MyColors.HudText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!isSignedIn) {
                        Text(
                            text = "Cloud synchronization disabled.",
                            color = MyColors.HudText.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }

                HorizontalDivider(color = MyColors.HudBlue.copy(alpha = 0.2f), thickness = 1.dp)

                if (!isSignedIn) {
                    // Button to show the Login/Register form inside the dialog
                    CyberButton(
                        onClick = {
                            showLoginForm = true
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, MyColors.HudBlue.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .background(MyColors.HudBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "LOGIN / REGISTER",
                                color = MyColors.HudBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // "OR" separator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MyColors.HudBlue.copy(alpha = 0.2f))
                        Text(
                            text = " OR ",
                            color = MyColors.HudText.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MyColors.HudBlue.copy(alpha = 0.2f))
                    }

                    // Google Login Icon (Clickable Image)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { onGoogleLogin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.android_dark),
                            contentDescription = "Continue with Google",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else {
                    // Logout Button
                    CyberButton(
                        onClick = {
                            onSignOut()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(45.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color.Red.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .background(Color.Red.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("TERMINATE SESSION (LOGOUT)", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

