package com.jax.roscontroller

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jax.roscontroller.ui.screens.HudIndicator
import com.jax.roscontroller.ui.theme.MyColors
import coil.compose.AsyncImage
import com.jax.roscontroller.ui.theme.toCyber


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
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = MyColors.HudBlue,
                    spotColor = MyColors.HudBlue
                )
                .background(MyColors.HudBackground.copy(alpha = 0.98f), RoundedCornerShape(12.dp))
                .border(1.dp, MyColors.HudBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Text(
                    text = title.uppercase(),
                    color = MyColors.HudBlue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        content()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Always show a Close/Cancel button
                    CyberButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "BACK",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (confirmText.isNotEmpty()) {
                        CyberButton(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, MyColors.HudBlue.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .background(MyColors.HudBlue.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    confirmText,
                                    color = MyColors.HudBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
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
    val scale by animateFloatAsState(if (isPressed && enabled) 0.96f else 1f, label = "buttonScale")
    val alpha by animateFloatAsState(if (isPressed && enabled) 0.8f else 1f, label = "buttonAlpha")

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = if (enabled) alpha else 0.5f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
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
        title = "SYSTEM_HELP",
        confirmText = "",
        onConfirm = {},
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {


            HelpSection("MINIMUM REQUIREMENTS".toCyber) {
                HelpBullet("rosbridge websocket (ws://<ip>:9090)")
                HelpBullet("geometry_msgs/Twist (motion)")
                HelpBullet("std_msgs/String (mode)")
            }

            HelpSection("ADD NEW ROBOT".toCyber) {
                HelpNote("Robot Tab")
                HelpBullet("Enter robot name and rosbridge IP:port (192.168.x.x:9090)")
                HelpBullet("Enter video stream URL (optional) or upload a robot image")
                HelpBullet("AXIS SETTINGS: Invert any direction if the joysticks move the robot the wrong way.")
                HelpBullet("NOTES: Add maintenance records, IP reminders, or hardware specs in the notes box.")
                
                HelpNote("Topics Tab")
                HelpBullet("Launch rosbridge on robot then tap 'Auto Discover Topics'")
                HelpBullet("Verify bindings: Most important are 'cmd_vel' and 'mode_topic'")
                HelpBullet("Telemetry: Bind Battery, IMU, Odom, CPU Temp, or Foot Sensors to see live data.")
                HelpBullet("Foot sensors require a Float32MultiArray (4 values: FL, FR, RL, RR).")

                HelpNote("Modes Tab")
                HelpBullet("Add custom commands (e.g., 'WALK', 'REST', 'SIT')")
                HelpBullet("These commands are sent as Strings to your configured Mode Topic.")
                HelpBullet("Double-check that the command string matches exactly what your robot expects.")
            }

            HelpSection("CONTROLS".toCyber) {
                HelpNote("Motion")
                HelpBullet("LEFT JOYSTICK: Controls Forward/Backward and Strafe movement.")
                HelpBullet("RIGHT JOYSTICK: Controls Turning (Left/Right).")
                HelpNote("Adjustments")
                HelpBullet("HGT SLIDER (Left): Controls body height (Z-axis offset).")
                HelpBullet("SPD SLIDER (Right): Global speed limiter (0-100%). Scales all movement and turning power.")
                HelpNote("Shortcuts")
                HelpBullet("DOUBLE-TAP SLIDER: Resets SPD to 100% or HGT to 0.00 instantly.")
                HelpBullet("MODE BUTTONS: Tap to switch between robot states (e.g., WALK, REST).")
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
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
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
fun ProfileDetailsContent(user: FirebaseUser) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var showLocationPrompt by remember { mutableStateOf(false) }
    var locationInput by remember { mutableStateOf("") }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(user.uid) {
        db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val p = snapshot.toObject(UserProfile::class.java)
                    profile = p
                    // If location is blank, show the quick popup
                    if (p != null && p.location.isBlank()) {
                        showLocationPrompt = true
                    }
                }
            }
    }

    if (showLocationPrompt) {
        CyberDialog(
            show = true,
            title = "LOCATION_REQUESTED",
            confirmText = "SAVE ▶",
            onConfirm = {
                if (locationInput.isNotBlank()) {
                    db.collection("users").document(user.uid)
                        .update("location", locationInput)
                    profile = profile?.copy(location = locationInput)
                    showLocationPrompt = false
                }
            },
            onDismiss = { showLocationPrompt = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Please provide your location so that you can be included in the interactive user map",
                    color = MyColors.HudText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                MyColors.HudTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = "COUNTRY / STATE"
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileInfoRow("EMAIL", (user.email ?: "N/A").toCyber)

        if (profile != null) {
            ProfileInfoRow("NAME", (if (profile?.displayName.isNullOrBlank()) "NOT SET" else profile?.displayName!!).toCyber)
            ProfileInfoRow("LOCATION", (if (profile?.location.isNullOrBlank()) "NOT SET" else profile?.location!!).toCyber)
        } else {
            Text(
                "Loading cloud data...",
                color = MyColors.HudText.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            color = MyColors.HudBlue.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value.uppercase(),
            color = MyColors.HudText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AccountStatusDialog(
    show: Boolean,
    user: com.google.firebase.auth.FirebaseUser?,
    onDismiss: () -> Unit,
    onGoogleLogin: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit = {}
) {
    if (!show) return

    var showLoginForm by remember { mutableStateOf(false) }
    var isLoginMode by remember { mutableStateOf(true) }
    var submitTrigger by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val isSignedIn = user != null
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    // Fetch profile for the main status view
    LaunchedEffect(user?.uid) {
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    profile = snapshot?.toObject(UserProfile::class.java)
                }
        } else {
            profile = null
        }
    }

    val title = when {
        showLoginForm -> "SYSTEM_AUTHENTICATION"
        isSignedIn -> "AUTH_SUCCESSFUL"
        else -> "GUEST_ACCESS"
    }

    val confirmText = if (showLoginForm && !isSignedIn) {
        if (isLoading) "PROCESSING..." else if (isLoginMode) "LOGIN ▶" else "CREATE ▶"
    } else ""

    if (showDeleteConfirmation) {
        CyberDialog(
            show = true,
            title = "CONFIRM_DESTRUCTION",
            confirmText = "DELETE_FOREVER",
            onConfirm = {
                showDeleteConfirmation = false
                onDeleteAccount()
            },
            onDismiss = { showDeleteConfirmation = false }
        ) {
            Text(
                text = "WARNING: This action is irreversible. All robot configurations and cloud-synced data will be permanently purged from the system.",
                color = MyColors.HudText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }

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
                    val profileImageUrl = profile?.photoPath?.takeIf { it.isNotBlank() } ?: user?.photoUrl
                    if (profileImageUrl != null) {
                        AsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, MyColors.HudBlue.copy(alpha = 0.6f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MyColors.HudBlue,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = (if (isSignedIn) {
                            if (!profile?.displayName.isNullOrBlank()) "IDENTIFIED:${profile?.displayName}"
                            else "IDENTIFIED: ${user?.email}"
                        } else {
                            "STATUS:UNREGISTERED"
                        })
                            .uppercase()            // 1. Force ALL CAPS
                            .replace(" ", "_"),     // 2. Swap spaces for underscores
                        color = MyColors.HudText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    if (isSignedIn) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ProfileDetailsContent(user!!)
                    }

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
                            modifier = Modifier.size(56.dp)
                        )
                    }
                } else {
                    // Logout Link
                    Text(
                        text = "TERMINATE SESSION (LOGOUT)",
                        color = MyColors.HudText.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                onSignOut()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    )

                    Text(
                        text = "DELETE ACCOUNT",
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { showDeleteConfirmation = true }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun RobotSelectionDialog(
    savedRobots: List<RobotConfig>,
    onDismiss: () -> Unit,
    onSelect: (RobotConfig) -> Unit
) {
    var selected by remember {
        val firstReal = savedRobots.firstOrNull { !it.name.contains("ROSbot", ignoreCase = true) }
        mutableStateOf(firstReal ?: savedRobots.firstOrNull())
    }

    CyberDialog(
        show = true,
        title = "Robot Selection".toCyber,
        confirmText = "LAUNCH ▶",
        onConfirm = { selected?.let { onSelect(it) } },
        onDismiss = onDismiss
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(savedRobots) { robot ->
                val isSelected = selected?.name == robot.name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MyColors.HudBlue.copy(alpha = 0.1f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MyColors.HudBlue else MyColors.HudBorder.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selected = robot }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { selected = robot },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MyColors.HudBlue,
                            unselectedColor = MyColors.HudText.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = robot.name,
                            color = if (isSelected) MyColors.HudBlue else MyColors.HudText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (robot.totalUptimeSeconds > 0) {
                            Text(
                                text = "TOTAL_UPTIME: ${formatUptime(robot.totalUptimeSeconds)}",
                                color = (if (isSelected) MyColors.HudBlue else MyColors.HudText).copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        if (selected?.name?.contains("ROSbot", ignoreCase = true) == true) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MyColors.HudBlue.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MyColors.HudBlue.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MyColors.HudBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NOTE: This is a demo profile to test the UI. Real-time hardware telemetry and video streaming will not be available until a live ROS bridge is configured.",
                        color = MyColors.HudText.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}


@Composable
fun MyColors.HudTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(
        capitalization = KeyboardCapitalization.None,
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next
    )
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        placeholder = {
            Text(
                text = "Enter ${label.lowercase()}...",
                color = MyColors.HudText.copy(alpha = 0.3f)
            )
        },
        textStyle = TextStyle(color = MyColors.HudText, fontSize = 14.sp),
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MyColors.HudBlue,
            unfocusedBorderColor = MyColors.HudText.copy(alpha = 0.4f),
            focusedTextColor = MyColors.HudText,
            unfocusedTextColor = MyColors.HudText,
            unfocusedLabelColor = MyColors.HudBlue,
            focusedLabelColor = MyColors.HudBlue,
            disabledLabelColor = MyColors.HudBlue
        ),
        shape = RoundedCornerShape(8.dp)
    )
}


