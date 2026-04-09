package com.example.jaxgamepad

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.example.jaxgamepad.ui.HudIndicator
import com.example.jaxgamepad.ui.JaxHudScreen
import com.example.jaxgamepad.ui.theme.JaxGamepadTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.example.jaxgamepad.ProfileScreen
import com.google.firebase.auth.FirebaseAuth





// --- UI Constants ---
val HudBlue = Color(0xFF0e7edb)
val HudBorder = Color(0xFF1A3A4A)
val HudBackground = Color(0xFF050B10)
val HudSurface = Color(0xFF0D1B26)
val HudText = Color(0xFFE6EEF5)
val Black = Color(0xFF000000)
val TerminalCyan = Color(0xFF8FE9FF)

// --- Network Helper Function ---



fun saveRobotConfigToFirestore(
    robot: RobotConfig,
    onResult: (String) -> Unit = {}
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid.isNullOrBlank()) {
        onResult("SAVED LOCALLY - SIGN IN TO SYNC")
        return
    }

    try {
        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "robotId" to robot.robotId,
            "ownerUid" to uid,
            "name" to robot.name,
            "rosAddress" to robot.rosAddress,
            "videoUrl" to robot.videoUrl,
            "thumbnailPath" to robot.thumbnailPath,
            "invertForwardBack" to robot.invertForwardBack,
            "invertStrafe" to robot.invertStrafe,
            "invertHeight" to robot.invertHeight,
            "invertTurn" to robot.invertTurn,
            "enabledIndicators" to robot.enabledIndicators,
            "totalUptimeSeconds" to robot.totalUptimeSeconds,
            "totalDistanceMeters" to robot.totalDistanceMeters,
            "updatedAt" to System.currentTimeMillis(),
            "cmdVelTopic_name" to robot.cmdVelTopic?.name,
            "cmdVelTopic_type" to robot.cmdVelTopic?.type,
            "modeTopic_name" to robot.modeTopic?.name,
            "modeTopic_type" to robot.modeTopic?.type,
            "batteryTopic_name" to robot.batteryTopic?.name,
            "batteryTopic_type" to robot.batteryTopic?.type,
            "imuTopic_name" to robot.imuTopic?.name,
            "imuTopic_type" to robot.imuTopic?.type,
            "odomTopic_name" to robot.odomTopic?.name,
            "odomTopic_type" to robot.odomTopic?.type,
            "jointStateTopic_name" to robot.jointStateTopic?.name,
            "jointStateTopic_type" to robot.jointStateTopic?.type,
            "modes" to robot.modes.map {
                hashMapOf(
                    "label" to it.label,
                    "command" to it.command
                )
            }
        )

        db.collection("users")
            .document(uid)
            .collection("robots")
            .document(robot.robotId)
            .set(data)
            .addOnSuccessListener {
                Log.d("FIRESTORE_ROBOT", "Saved robot: ${robot.name} (${robot.robotId})")
                onResult("- CLOUD SYNC SUCCESSFUL")
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_ROBOT", "Failed saving robot", e)
                onResult("CLOUD SYNC FAILED\n${e.message ?: "Unknown error"}")
            }
    } catch (e: Exception) {
        Log.e("FIRESTORE_ROBOT", "Exception saving robot", e)
        onResult("CLOUD SYNC FAILED\n${e.message ?: "Unknown error"}")
    }
}

fun deleteRobotConfigFromFirestore(
    robot: RobotConfig,
    onResult: (String) -> Unit = {}
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid.isNullOrBlank()) {
        onResult("REMOVED LOCALLY")
        return
    }

    try {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("robots")
            .document(robot.robotId)
            .delete()
            .addOnSuccessListener {
                Log.d("FIRESTORE_ROBOT", "Deleted robot: ${robot.name} (${robot.robotId})")
                onResult("REMOVED LOCAL + CLOUD")
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_ROBOT", "Failed deleting robot", e)
                onResult("REMOVED LOCAL - CLOUD DELETE FAILED\n${e.message ?: "Unknown error"}")
            }
    } catch (e: Exception) {
        Log.e("FIRESTORE_ROBOT", "Exception deleting robot", e)
        onResult("REMOVED LOCAL - CLOUD DELETE FAILED\n${e.message ?: "Unknown error"}")
    }
}

private fun syncRobotsToFirestoreForSignedInUser(robots: List<RobotConfig>) {
    if (robots.isEmpty()) return
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    robots.filterNot { it.isDemoRobot() }.forEach { robot ->
        saveRobotConfigToFirestore(robot.copy(ownerUid = uid))
    }
}

private fun fetchRobotsFromFirestoreForSignedInUser(
    uid: String,
    onResult: (List<RobotConfig>) -> Unit,
    onFailure: (Exception) -> Unit = {}
) {
    FirebaseFirestore.getInstance()
        .collection("users")
        .document(uid)
        .collection("robots")
        .get()
        .addOnSuccessListener { snapshot ->
            try {
                val robots = snapshot.documents.mapNotNull { doc ->
                    val robotId = doc.getString("robotId")?.takeIf { it.isNotBlank() } ?: doc.id
                    val name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

                    val modes = (doc.get("modes") as? List<*>)
                        ?.mapNotNull { modeEntry ->
                            val modeMap = modeEntry as? Map<*, *> ?: return@mapNotNull null
                            val label = modeMap["label"] as? String ?: return@mapNotNull null
                            val command = modeMap["command"] as? String ?: return@mapNotNull null
                            if (label.isBlank() || command.isBlank()) null else RobotMode(label, command)
                        }
                        ?: emptyList()

                    val enabledIndicators = (doc.get("enabledIndicators") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.takeIf { it.isNotEmpty() }
                        ?: HudIndicator.entries.map { it.name }

                    fun topicBinding(prefix: String): TopicBinding? {
                        val topicName = doc.getString("${prefix}_name")?.trim().orEmpty()
                        val topicType = doc.getString("${prefix}_type")?.trim().orEmpty()
                        return if (topicName.isNotBlank() && topicType.isNotBlank()) {
                            TopicBinding(name = topicName, type = topicType)
                        } else {
                            null
                        }
                    }

                    RobotConfig(
                        name = name,
                        rosAddress = doc.getString("rosAddress") ?: "",
                        videoUrl = doc.getString("videoUrl") ?: "",
                        thumbnailPath = doc.getString("thumbnailPath"),
                        cmdVelTopic = topicBinding("cmdVelTopic"),
                        modeTopic = topicBinding("modeTopic"),
                        batteryTopic = topicBinding("batteryTopic"),
                        imuTopic = topicBinding("imuTopic"),
                        odomTopic = topicBinding("odomTopic"),
                        jointStateTopic = topicBinding("jointStateTopic"),
                        modes = modes,
                        enabledIndicators = enabledIndicators,
                        totalUptimeSeconds = doc.getLong("totalUptimeSeconds") ?: 0L,
                        totalDistanceMeters = doc.getDouble("totalDistanceMeters") ?: 0.0,
                        invertForwardBack = doc.getBoolean("invertForwardBack") ?: false,
                        invertStrafe = doc.getBoolean("invertStrafe") ?: false,
                        invertHeight = doc.getBoolean("invertHeight") ?: false,
                        invertTurn = doc.getBoolean("invertTurn") ?: false,
                        robotId = robotId,
                        ownerUid = uid
                    )
                }
                onResult(robots)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
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
fun HelpSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = HudBlue,
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
        color = HudText,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}

@Composable
fun HelpNote(text: String) {
    Text(
        text = text,
        color = HudText,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        textDecoration = TextDecoration.Underline


    )
}

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
                    ambientColor = HudBlue, // Explicitly named to avoid type mismatch
                    spotColor = HudBlue    // Explicitly named
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(HudBlue, Color(0xFF008CFF), HudBlue)
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
                    .heightIn(max = 560.dp)
            ) {
                Text(
                    text = title,
                    color = HudText,
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
                        Text("DISMISS", color = HudText.copy(alpha = 0.75f))
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
                                    .border(1.dp, HudBlue, RoundedCornerShape(10.dp))
                                    .background(HudBlue.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(confirmText, color = HudBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AppNavigation(
                    reHideSystemBars = {
                        val controller = WindowInsetsControllerCompat(window, window.decorView)
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                )
            }
        }
    }
}

enum class Screen { Menu, Gamepad, RobotSetup, Account }


@Composable
fun GlobalTopBar(onOpenAccount: () -> Unit, onOpenHelp: () -> Unit) {
    Surface(
        color = Color.Black,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp) // Tighter vertical padding
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Help Icon on the Left
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(HudBlue.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, HudBlue.copy(alpha = 0.4f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenHelp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = "Help",
                    tint = HudBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Profile Icon on the Right
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(HudBlue.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, HudBlue.copy(alpha = 0.4f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenAccount
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = HudBlue,
                    modifier = Modifier.size(20.dp) // Smaller icon
                )
            }
        }
    }
}
@Composable
fun AppNavigation(reHideSystemBars: () -> Unit) {
    val context = LocalContext.current
    val robotManager = remember { RobotManager(context) }

    var signedInUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    var terminalText by remember { mutableStateOf("") }
    var hasBooted by remember { mutableStateOf(false) }

    val networkInfo by produceState(initialValue = "SCANNING" to "0.0.0.0") {
        while (true) {
            value = getNetworkDetails(context)
            delay(5000)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasBooted) {
            val (status, addr) = networkInfo
            val isConnected = status == "WIFI_CONNECTED"
            val userEmail = signedInUser?.email ?: "GUEST"

            val script = if (isConnected) {
                // Standard success boot
                "> BOOTING_ROS_CONTROLLER" +
                        "\n|>" +
                        "\n|>" +
                        "\n|> USER: $userEmail" +
                        "\n|> LINK: $status" +
                        "\n|> IP: $addr" +
                        "\n|>" +
                        "\n|>" +
                        "\n|> STATUS: SYSTEM_READY_"
            } else {
                // Failure boot sequence
                "> BOOTING_ROS_CONTROLLER" +
                        "\n|>" +
                        "\n|>" +
                        "\n|> USER: $userEmail" +
                        "\n|> LINK: $status" +
                        "\n|> IP: UNKNOWN" +
                        "\n|>" +
                        "\n|> ERROR: NO_LOCAL_IP_FOUND" +
                        "\n|>" +
                        "\n|> STATUS: SYSTEM_OFFLINE"
            }

            val chunks = script.split("|")
            chunks.forEachIndexed { index, chunk ->
                chunk.forEach { char ->
                    terminalText += char
                    delay(30)
                }
                if (index < chunks.size - 1) delay(300)
            }
            hasBooted = true
        }
    }

    LaunchedEffect(networkInfo, signedInUser) {
        if (hasBooted) {
            val (status, addr) = networkInfo
            val isConnected = status == "WIFI_CONNECTED"
            val userEmail = signedInUser?.email ?: "GUEST"

            terminalText = if (isConnected) {
                "> BOOTING_ROS_CONTROLLER" +
                        "\n|>" +
                        "\n|>" +
                        "\n|> USER: $userEmail" +
                        "\n|> LINK: $status" +
                        "\n|> IP: $addr" +
                        "\n|>" +
                        "\n|>" +
                        "\n|> STATUS: SYSTEM_READY_"
            } else {
                // Failure boot sequence
                "> BOOTING_ROS_CONTROLLER" +
                        "\n|>" +
                        "\n|>" +
                        "\n|> USER: $userEmail" +
                        "\n|> LINK: $status" +
                        "\n|> IP: UNKNOWN" +
                        "\n|>" +
                        "\n|> ERROR: NO_LOCAL_IP_FOUND" +
                        "\n|>" +
                        "\n|> STATUS: SYSTEM_OFFLINE"
            }
        }
    }

    fun buildDemoRobot(ownerUid: String = RobotManager.GUEST_OWNER_UID): RobotConfig {
        return RobotConfig(
            name = "ROSbot (Demo)",
            rosAddress = "192.168.1.XX",
            videoUrl = "http://192.168.1.XX:8080/stream?topic=/camera/image_raw",
            thumbnailPath = "demo_thumb",
            cmdVelTopic = TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
            modeTopic = TopicBinding("/jax_mode", "std_msgs/String"),
            jointStateTopic = TopicBinding("/joint_states", "sensor_msgs/JointState"),
            modes = listOf(
                RobotMode("STAND", "stand"),
                RobotMode("WALK", "walk"),
                RobotMode("LAY", "lay"),
                RobotMode("SHAKE", "shake"),
                RobotMode("SIT", "sit"),
                RobotMode("WAVE", "wave"),
            ),
            invertForwardBack = false,
            invertStrafe = true,
            invertHeight = false,
            invertTurn = true,
            ownerUid = ownerUid
        )
    }

    fun loadRobotsForOwner(ownerUid: String?): List<RobotConfig> {
        val normalizedOwner = RobotManager.normalizeOwnerUid(ownerUid)
        val loaded = robotManager.loadRobots(normalizedOwner)
        val needsDemo = loaded.isEmpty() || loaded.any { it.name.lowercase() == "jax-1" }

        if (!needsDemo) {
            return loaded
        }

        val demo = buildDemoRobot(normalizedOwner)
        val cleaned = loaded.filterNot { it.name.lowercase() == "jax-1" }
        val withDemo = if (cleaned.any { it.isDemoRobot() }) cleaned else listOf(demo) + cleaned

        robotManager.saveRobots(withDemo, normalizedOwner)
        return withDemo
    }

    var savedRobots by remember {
        mutableStateOf(loadRobotsForOwner(signedInUser?.uid))
    }

    var currentScreen by remember { mutableStateOf(Screen.Menu) }
    var currentRobot by remember {
        mutableStateOf(savedRobots.firstOrNull() ?: buildDemoRobot(RobotManager.normalizeOwnerUid(signedInUser?.uid)))
    }
    var hapticsEnabled by remember { mutableStateOf(true) }

    val ros = remember { RosbridgeClient() }
    val activity = LocalContext.current as? ComponentActivity

    val portraitScreens = setOf(Screen.Menu, Screen.RobotSetup, Screen.Account)

    LaunchedEffect(currentScreen) {
        activity?.let {
            if (currentScreen in portraitScreens) {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                WindowInsetsControllerCompat(it.window, it.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            } else {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                reHideSystemBars()
            }
        }
    }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            signedInUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(signedInUser?.uid) {
        val ownerUid = signedInUser?.uid

        if (ownerUid.isNullOrBlank()) {
            val guestRobots = loadRobotsForOwner(RobotManager.GUEST_OWNER_UID)
            savedRobots = guestRobots
            currentRobot = guestRobots.firstOrNull { it.robotId == currentRobot.robotId }
                ?: guestRobots.firstOrNull()
                        ?: buildDemoRobot(RobotManager.GUEST_OWNER_UID)
            return@LaunchedEffect
        }

        fetchRobotsFromFirestoreForSignedInUser(
            uid = ownerUid,
            onResult = { cloudRobots ->
                val cloudBase = cloudRobots
                    .filterNot { it.isDemoRobot() }
                    .associateBy { it.robotId }
                    .toMutableMap()

                val guestRobots = robotManager.loadRobots(RobotManager.GUEST_OWNER_UID)
                    .filterNot { it.isDemoRobot() }

                guestRobots.forEach { guestRobot ->
                    val migrated = guestRobot.copy(ownerUid = ownerUid)
                    val duplicateCloud = cloudBase.values.any {
                        it.name.equals(migrated.name, ignoreCase = true)
                    }
                    if (!cloudBase.containsKey(migrated.robotId) && !duplicateCloud) {
                        cloudBase[migrated.robotId] = migrated
                    }
                }

                val merged = cloudBase.values.toList()
                val updatedRobots = if (merged.isEmpty()) {
                    loadRobotsForOwner(ownerUid)
                } else {
                    val withDemo = if (merged.any { it.isDemoRobot() }) merged else listOf(buildDemoRobot(ownerUid)) + merged
                    robotManager.saveRobots(withDemo, ownerUid)
                    robotManager.clearOwner(RobotManager.GUEST_OWNER_UID)
                    syncRobotsToFirestoreForSignedInUser(withDemo)
                    withDemo
                }

                savedRobots = updatedRobots
                currentRobot = updatedRobots.firstOrNull { it.robotId == currentRobot.robotId }
                    ?: updatedRobots.firstOrNull()
                            ?: buildDemoRobot(ownerUid)
            },
            onFailure = {
                val merged = robotManager.mergeGuestRobotsIntoOwner(ownerUid)
                val withDemo = if (merged.isEmpty()) loadRobotsForOwner(ownerUid) else merged
                syncRobotsToFirestoreForSignedInUser(withDemo)
                savedRobots = withDemo
                currentRobot = withDemo.firstOrNull { it.robotId == currentRobot.robotId }
                    ?: withDemo.firstOrNull()
                            ?: buildDemoRobot(ownerUid)
            }
        )
    }

    var showGlobalHelp by remember { mutableStateOf(false) }

    if (showGlobalHelp) {
        HelpDialog(show = true, onDismiss = { showGlobalHelp = false })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (currentScreen != Screen.Gamepad) {
            GlobalTopBar(
                onOpenAccount = {
                    currentScreen = Screen.Account
                },
                onOpenHelp = {
                    showGlobalHelp = true
                }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (currentScreen) {
                Screen.Menu -> StartMenuScreen(
                    ros = ros,
                    savedRobots = savedRobots,
                    terminalText = terminalText,
                    isSignedIn = signedInUser != null,
                    signedInLabel = signedInUser?.email ?: "Signed in",
                    onLaunchGamepad = { robot ->
                        currentRobot = robot
                        currentScreen = Screen.Gamepad
                    },
                    onLaunchSetup = { currentScreen = Screen.RobotSetup },
                    onOpenAccount = {
                        currentScreen = Screen.Account
                    }
                )

                Screen.Account -> AccountScreen(
                    onBack = {
                        currentScreen = Screen.Menu
                    }
                )

                Screen.Gamepad -> JaxDriverScreen(
                    ros = ros,
                    currentRobot = currentRobot,
                    savedRobots = savedRobots,
                    hapticsEnabled = hapticsEnabled,
                    onRobotChange = { newRobot ->
                        currentRobot = newRobot
                        val ownerUid = RobotManager.normalizeOwnerUid(signedInUser?.uid)
                        val normalizedRobot = newRobot.copy(ownerUid = ownerUid)
                        val list = savedRobots.toMutableList()
                        val idx = list.indexOfFirst { it.robotId == normalizedRobot.robotId }
                        if (idx != -1) {
                            list[idx] = normalizedRobot
                            savedRobots = list
                            robotManager.saveRobots(list, ownerUid)
                            saveRobotConfigToFirestore(normalizedRobot)
                        }
                    },
                    onHapticsChange = { hapticsEnabled = it },
                    reHideSystemBars = reHideSystemBars,
                    onBackToMenu = { currentScreen = Screen.Menu },
                    networkInfo = networkInfo
                )

                Screen.RobotSetup -> RobotSetupScreen(
                    ros = ros,
                    savedRobots = savedRobots,
                    onSave = { oldName, newRobot ->
                        val ownerUid = RobotManager.normalizeOwnerUid(signedInUser?.uid)
                        val normalizedRobot = newRobot.copy(ownerUid = ownerUid)
                        val list = savedRobots.toMutableList()
                        val index = list.indexOfFirst { it.robotId == normalizedRobot.robotId }
                            .takeIf { it != -1 }
                            ?: list.indexOfFirst { it.name == oldName }

                        if (index != -1) {
                            list[index] = normalizedRobot
                        } else {
                            list.add(normalizedRobot)
                        }

                        savedRobots = list
                        robotManager.saveRobots(list, ownerUid)

                        if (currentRobot.robotId == normalizedRobot.robotId || currentRobot.name == oldName) {
                            currentRobot = normalizedRobot
                        }
                    },
                    onDelete = { robot ->
                        val ownerUid = RobotManager.normalizeOwnerUid(signedInUser?.uid)
                        val list = savedRobots.filter { it.robotId != robot.robotId }
                        savedRobots = list
                        robotManager.saveRobots(list, ownerUid)
                        deleteRobotConfigFromFirestore(robot)
                        currentRobot = list.firstOrNull() ?: buildDemoRobot(ownerUid)
                    },
                    onBack = { currentScreen = Screen.Menu },
                    onOpenAccount = {
                        currentScreen = Screen.Account
                    }
                )
            }
        }
    }
}

@Composable
fun JaxDriverScreen(
    ros: RosbridgeClient,
    currentRobot: RobotConfig,
    savedRobots: List<RobotConfig>,
    hapticsEnabled: Boolean,
    onRobotChange: (RobotConfig) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    reHideSystemBars: () -> Unit,
    onBackToMenu: () -> Unit,
    networkInfo: Pair<String, String>
) {
    var moveX by remember { mutableStateOf(0.0) }
    var moveY by remember { mutableStateOf(0.0) }
    var turnZ by remember { mutableStateOf(0.0) }
    var bodyHeightZ by remember { mutableStateOf(0.0) }   // -1.0 .. 1.0
    var bodyRollX by remember { mutableStateOf(0.0) }     // for rest mode later
    var bodyPitchY by remember { mutableStateOf(0.0) }    // for rest mode later
    var showSettings by remember { mutableStateOf(false) }
    var showTerminateVerify by remember { mutableStateOf(false) }
    var reconnectNonce by remember { mutableIntStateOf(0) }

    ros.isConnected
    val sessionBaseUptime = remember(currentRobot.name) { currentRobot.totalUptimeSeconds }
    var sessionSeconds by remember(currentRobot.name) { mutableLongStateOf(0L) }
    var sessionRunning by remember(currentRobot.name) { mutableStateOf(false) }
    var lastSavedSessionSeconds by remember(currentRobot.name) { mutableLongStateOf(0L) }
    var wasConnected by remember(currentRobot.name) { mutableStateOf(false) }
    val liveSessionText = formatUptime(sessionSeconds)
    val activeIndicators = remember(currentRobot) {
        currentRobot.enabledIndicators.mapNotNull {
            try {
                HudIndicator.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }.toSet()
    }

    var videoLoadedManually by remember(currentRobot.name) { mutableStateOf(false) }
    var videoError by remember(currentRobot.videoUrl) { mutableStateOf<String?>(null) }
    var videoButtonActive by remember(currentRobot.videoUrl) { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(currentRobot.rosAddress, reconnectNonce) {
        if (currentRobot.rosAddress.isNotBlank()) {
            ros.disconnect()
            ros.connect("ws://${currentRobot.rosAddress}") {
                ros.advertiseIfNeeded(currentRobot)
                ros.subscribeToTelemetry(currentRobot)
            }
        }
    }

    DisposableEffect(currentRobot) {
        onDispose { ros.clearTelemetrySubscriptions(currentRobot) }
    }

    DisposableEffect(
        ros.isConnected,
        moveX,
        moveY,
        turnZ,
        bodyHeightZ,
        bodyRollX,
        bodyPitchY,
        currentRobot
    ) {
        val linearX = if (currentRobot.invertForwardBack) -moveY else moveY
        val linearY = if (currentRobot.invertStrafe) -moveX else moveX
        val linearZ = if (currentRobot.invertHeight) -bodyHeightZ else bodyHeightZ
        val angularZ = if (currentRobot.invertTurn) -turnZ else turnZ

        val publishJob: Job? = if (ros.isConnected) {
            scope.launch {
                while (true) {
                    ros.publishCmdVel(
                        robot = currentRobot,
                        linearX = linearX,
                        linearY = linearY,
                        linearZ = linearZ,
                        angularX = bodyRollX,
                        angularY = bodyPitchY,
                        angularZ = angularZ
                    )

                    delay(75)
                }
            }
        } else {
            null
        }
        onDispose { publishJob?.cancel() }
    }

    LaunchedEffect(ros.isConnected, currentRobot.name) {
        if (ros.isConnected) {
            sessionRunning = true
            wasConnected = true
        } else {
            sessionRunning = false

            if (wasConnected && sessionSeconds > lastSavedSessionSeconds) {
                onRobotChange(
                    currentRobot.copy(
                        totalUptimeSeconds = sessionBaseUptime + sessionSeconds
                    )
                )
                lastSavedSessionSeconds = sessionSeconds
            }

            wasConnected = false
        }
    }

    LaunchedEffect(sessionRunning, currentRobot.name) {
        while (sessionRunning) {
            delay(1000)
            sessionSeconds += 1L

            if (sessionSeconds - lastSavedSessionSeconds >= 10L) {
                onRobotChange(
                    currentRobot.copy(
                        totalUptimeSeconds = sessionBaseUptime + sessionSeconds
                    )
                )
                lastSavedSessionSeconds = sessionSeconds
            }
        }
    }

    DisposableEffect(currentRobot.name) {
        onDispose {
            if (sessionSeconds > lastSavedSessionSeconds) {
                onRobotChange(
                    currentRobot.copy(
                        totalUptimeSeconds = sessionBaseUptime + sessionSeconds
                    )
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            savedRobots = savedRobots,
            currentRobot = currentRobot,
            initialHaptics = hapticsEnabled,
            activeIndicators = activeIndicators,
            onToggleIndicator = { ind ->
                val newSet = if (activeIndicators.contains(ind)) {
                    activeIndicators - ind
                } else {
                    activeIndicators + ind
                }
                onRobotChange(currentRobot.copy(enabledIndicators = newSet.map { it.name }))
            },
            onDismiss = {
                showSettings = false
                reHideSystemBars()
            },
            onSave = { robot, haptics ->
                onRobotChange(robot)
                onHapticsChange(haptics)
                showSettings = false
                reHideSystemBars()
            },
            onDisconnect = { ros.disconnect() },
            onBackToMenu = onBackToMenu
        )
    }

    if (showTerminateVerify) {
        val powerActionText = if (ros.isConnected) "" else "RECONNECT ▶"

        CyberDialog(
            show = showTerminateVerify,
            title = "POWER OPTIONS",
            confirmText = powerActionText,
            onConfirm = {
                if (!ros.isConnected) {
                    ros.disconnect()
                    reconnectNonce++
                    showTerminateVerify = false
                    reHideSystemBars()
                }
            },
            onDismiss = {
                showTerminateVerify = false
                reHideSystemBars()
            }
        ) {
            Text(
                text = if (ros.isConnected) {
                    "ROS link is active. System is responding normally."
                } else {
                    "ROS link is offline. Link lost at ${networkInfo.second}."
                },
                color = HudText,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            CyberButton(
                onClick = {
                    showTerminateVerify = false
                    onBackToMenu()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Red.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .background(Color.Red.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TERMINATE SESSION & EXIT",
                        color = Color.Red.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    JaxHudScreen(
        robotName = currentRobot.name,
        sessionTimeText = liveSessionText,
        batteryPercent = ros.lastBatteryPercent ?: 0,
        selectedMode = ros.lastModeText ?: currentRobot.modes.firstOrNull()?.command ?: "walk",
        modes = currentRobot.modes,
        enabledIndicators = activeIndicators,
        leftJoystickValue = moveX.toFloat() to moveY.toFloat(),
        rightJoystickValue = turnZ.toFloat() to 0f,
        heightSliderValue = bodyHeightZ.toFloat(),
        videoActive = videoButtonActive,
        hapticsEnabled = hapticsEnabled,
        isLinked = ros.isConnected,
        onVideoToggle = { enabled ->
            videoButtonActive = enabled
            videoLoadedManually = enabled
            videoError = null
        },
        onLeftJoystickChanged = { x, y ->
            moveX = x.toDouble()
            moveY = y.toDouble()
        },
        onRightJoystickChanged = { x, _ ->
            turnZ = x.toDouble()
        },
        onHeightSliderChanged = { z ->
            bodyHeightZ = z.toDouble()
        },
        onModeSelected = { mode -> ros.publishMode(currentRobot, mode) },
        onSettingsClick = { showSettings = true },
        onTerminateClick = { showTerminateVerify = true },
        videoFeed = {
            VideoFeedContainer(
                modifier = Modifier.fillMaxSize(),
                hatchOpen = videoButtonActive && videoLoadedManually,
                errorText = videoError,
                videoUrl = currentRobot.videoUrl
            ) {
                if (videoButtonActive) {
                    MjpegWebView(
                        url = currentRobot.videoUrl,
                        onLoadingStateChanged = { _, error ->
                            videoError = error
                        },
                        onUserInteraction = reHideSystemBars
                    )
                }
            }
        }
    )
}

@Composable
fun RobotSetupScreen(
    ros: RosbridgeClient,
    savedRobots: List<RobotConfig>,
    onSave: (String?, RobotConfig) -> Unit,
    onDelete: (RobotConfig) -> Unit,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    initialEditingRobot: RobotConfig? = null,
    initialIsAdding: Boolean = false,
    initialSelectedTabOrStep: Int = 0
) {
    val context = LocalContext.current
    var editingRobot by remember(initialEditingRobot) { mutableStateOf(initialEditingRobot) }
    var isAdding by remember(initialIsAdding) { mutableStateOf(initialIsAdding) }
    var robotToDelete by remember { mutableStateOf<RobotConfig?>(null) }
    var modeToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var showRosRequirements by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var firestoreStatus by remember { mutableStateOf("") }
    var showCloudResultDialog by remember { mutableStateOf(false) }
    var pendingCloseAfterCloudSave by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var selectedTabOrStep by remember { mutableIntStateOf(initialSelectedTabOrStep) }
    var maxStepReached by remember(isAdding) { mutableIntStateOf(initialSelectedTabOrStep) }

    val hasNoUserRobots =
        savedRobots.size <= 1 && savedRobots.any { it.name.contains("Demo", ignoreCase = true) }

    CyberDialog(
        show = showRosRequirements,
        title = "MINIMUM REQUIREMENTS",
        confirmText = "PROCEED ▶",
        onConfirm = {
            showRosRequirements = false
            isAdding = true
            selectedTabOrStep = 0
            maxStepReached = 0
        },
        onDismiss = { showRosRequirements = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Minimum Robot Requirements:",
                color = HudBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text("• Run robridge - ws://192.168.x.x:9090", color = HudText, fontSize = 13.sp)
            Text("• Motion topic using - geometry_msgs/Twist", color = HudText, fontSize = 13.sp)
            Text("• Mode topic using - std_msgs/String", color = HudText, fontSize = 13.sp)
            Text("Optional:", color = HudBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("• MJPEG stream for video support", color = HudText, fontSize = 13.sp)
        }
    }

    if (robotToDelete != null) {
        CyberDialog(
            show = true,
            title = "AUTHORIZE DELETION",
            confirmText = "DELETE ▶",
            onConfirm = {
                robotToDelete?.let { onDelete(it) }
                robotToDelete = null
            },
            onDismiss = { robotToDelete = null }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Are you sure you want to purge the configuration for '${robotToDelete?.name}' from the local database?",
                    color = HudText,
                    fontSize = 14.sp
                )

                Text(
                    text = "WARNING: THIS ACTION CANNOT BE UNDONE.",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    if (showHelp) {
        HelpDialog(
            show = true,
            onDismiss = { showHelp = false }
        )
    }

    if (showCloudResultDialog) {
        CyberDialog(
            show = true,
            title = "SYSTEM MESSAGE",
            confirmText = "",
            onConfirm = {
                showCloudResultDialog = false

                if (pendingCloseAfterCloudSave) {
                    editingRobot = null
                    isAdding = false
                    selectedTabOrStep = 0
                    pendingCloseAfterCloudSave = false
                }
            },
            onDismiss = {
                showCloudResultDialog = false

                if (pendingCloseAfterCloudSave) {
                    editingRobot = null
                    isAdding = false
                    selectedTabOrStep = 0
                    pendingCloseAfterCloudSave = false
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "- LOCAL SAVE SUCCESSFUL",
                    color = HudBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = firestoreStatus,
                    color = if (firestoreStatus.startsWith("CLOUD SYNC FAILED")) Color.Red else HudBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBackground)

    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_app),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )



        if (editingRobot == null && !isAdding) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(15.dp))
                Image(
                    painter = painterResource(id = R.drawable.saved_robots),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .fillMaxWidth(.80f)
                        .wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.padding(end = 6.dp),
                            text = "ADD NEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HudBlue,
                            letterSpacing = 1.sp
                        )

                        IconButton(
                            onClick = {
                                if (hasNoUserRobots) {
                                    showRosRequirements = true
                                } else {
                                    isAdding = true
                                    selectedTabOrStep = 0
                                    maxStepReached = 0
                                }
                            },
                            modifier = Modifier
                                .background(HudBlue.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, HudBlue.copy(alpha = 0.4f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add New",
                                tint = HudBlue
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedRobots) { robot ->
                        RobotListItem(
                            robot = robot,
                            onEdit = {
                                editingRobot = robot
                                isAdding = false
                                selectedTabOrStep = 0
                            },
                            onDelete = { robotToDelete = robot }
                        )
                    }

                    if (hasNoUserRobots) {
                        item {
                            Surface(
                                color = HudBlue.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, HudBlue.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = HudBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "SYSTEM NOTIFICATION",
                                            color = HudBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "We have provided a sample robot (ROSbot) to test the UI. Configure your own hardware profile to enable real-time telemetry and motion control.",
                                            color = HudText.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CyberButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.back_button),
                        contentDescription = "Back",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(25.dp))
            }
        } else {
            val initial = editingRobot ?: RobotConfig("", "", "")
            var name by remember(editingRobot) { mutableStateOf(initial.name) }
            var addr by remember(editingRobot) { mutableStateOf(initial.rosAddress) }
            var url by remember(editingRobot) { mutableStateOf(initial.videoUrl) }

            var invertForwardBack by remember(editingRobot) { mutableStateOf(initial.invertForwardBack) }
            var invertStrafe by remember(editingRobot) { mutableStateOf(initial.invertStrafe) }
            var invertHeight by remember(editingRobot) { mutableStateOf(initial.invertHeight) }
            var invertTurn by remember(editingRobot) { mutableStateOf(initial.invertTurn) }

            var selectedThumbnailUri by remember(editingRobot) {
                mutableStateOf<Uri?>(
                    if (initial.thumbnailPath != null && initial.thumbnailPath != "demo_thumb") {
                        Uri.fromFile(File(initial.thumbnailPath))
                    } else {
                        null
                    }
                )
            }

            var allDiscoveredTopics by remember { mutableStateOf<List<RosTopicInfo>>(emptyList()) }
            var cmdVelTopic by remember(editingRobot) { mutableStateOf(initial.cmdVelTopic) }
            var modeTopic by remember(editingRobot) { mutableStateOf(initial.modeTopic) }
            var batteryTopic by remember(editingRobot) { mutableStateOf(initial.batteryTopic) }
            var imuTopic by remember(editingRobot) { mutableStateOf(initial.imuTopic) }
            var odomTopic by remember(editingRobot) { mutableStateOf(initial.odomTopic) }
            var jointStateTopic by remember(editingRobot) { mutableStateOf(initial.jointStateTopic) }

            val modes = remember(editingRobot) {
                mutableStateListOf<RobotMode>().apply { addAll(initial.modes) }
            }
            var discoverStatus by remember { mutableStateOf<String?>(null) }
            var discovering by remember { mutableStateOf(false) }

            val tabsOrSteps = listOf("ROBOT", "TOPICS", "MODES")
            var showModeDialog by remember { mutableStateOf(false) }
            var modeToEditIndex by remember { mutableStateOf<Int?>(null) }

            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) selectedThumbnailUri = uri
            }

            if (showModeDialog) {
                ModeEditDialog(
                    initialMode = if (modeToEditIndex != null) modes[modeToEditIndex!!] else null,
                    onDismiss = {
                        showModeDialog = false
                        modeToEditIndex = null
                    },
                    onSave = { updatedMode ->
                        if (modeToEditIndex != null) {
                            modes[modeToEditIndex!!] = updatedMode
                        } else if (modes.none {
                                it.command.equals(updatedMode.command, ignoreCase = true)
                            }
                        ) {
                            modes.add(updatedMode)
                        }
                        showModeDialog = false
                        modeToEditIndex = null
                    }
                )
            }

            if (modeToDeleteIndex != null) {
                val modeName = modes.getOrNull(modeToDeleteIndex!!)?.label ?: "Unknown Mode"

                CyberDialog(
                    show = true,
                    title = "AUTHORIZE DELETION",
                    confirmText = "DELETE ▶",
                    onConfirm = {
                        modeToDeleteIndex?.let { modes.removeAt(it) }
                        modeToDeleteIndex = null
                    },
                    onDismiss = { modeToDeleteIndex = null }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Are you sure you want to remove the '$modeName' command from the controller interface?",
                            color = HudText,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "WARNING: THIS WILL REMOVE THE BUTTON FROM THE HUD AND WILL NEED TO BE RECREATED IF NEEDED.",
                            color = Color.Red.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(15.dp))
                Image(
                    painter = painterResource(
                        id = if (isAdding) R.drawable.new_robot else R.drawable.saved_robots
                    ),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .fillMaxWidth(.80f)
                        .wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {

                    TabRow(
                        selectedTabIndex = selectedTabOrStep,
                        containerColor = Color.Transparent,
                        contentColor = HudBlue,
                        indicator = { tabPositions ->
                            if (selectedTabOrStep < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabOrStep]),
                                    color = HudBlue
                                )
                            }
                        },
                        divider = {}
                    ) {
                        tabsOrSteps.forEachIndexed { index, title ->
                            val isEnabled = !isAdding || index <= maxStepReached
                            Tab(
                                selected = selectedTabOrStep == index,
                                onClick = { if (isEnabled) selectedTabOrStep = index },
                                enabled = isEnabled,
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEnabled) HudText else HudBlue
                                    )
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    when (selectedTabOrStep) {
                        0 -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .border(
                                            width = 1.dp,
                                            color = HudText.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedThumbnailUri != null) {
                                        AsyncImage(
                                            model = selectedThumbnailUri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (initial.thumbnailPath == "demo_thumb") {
                                        Image(
                                            painter = painterResource(id = R.drawable.jax_demo_icon),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = HudText.copy(alpha = 0.4f)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "UPTIME : ${formatUptime(initial.totalUptimeSeconds)}",
                                        color = HudText.copy(alpha = .7f),
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp
                                    )

                                    Text(
                                        text = "TRAVELED: ${formatDistance(initial.totalDistanceMeters)}",
                                        color = HudText.copy(alpha = .7f),
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            HudTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = "ROBOT NAME *"
                            )
                            HudTextField(
                                value = addr,
                                onValueChange = { addr = it },
                                label = "ROS BRIDGE IP (IP:PORT) *"
                            )
                            HudTextField(
                                value = url,
                                onValueChange = { url = it },
                                label = "VIDEO SERVER URL (OPTIONAL)"
                            )


                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "AXIS SETTINGS",
                                color = HudBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            AxisSwitchRowDual(
                                left = {
                                    AxisSwitchCompact(
                                        title = "FORWARD",
                                        checked = invertForwardBack,
                                        onCheckedChange = { invertForwardBack = it }
                                    )
                                },
                                right = {
                                    AxisSwitchCompact(
                                        title = "STRAFE",
                                        checked = invertStrafe,
                                        onCheckedChange = { invertStrafe = it }
                                    )
                                }
                            )

                            AxisSwitchRowDual(
                                left = {
                                    AxisSwitchCompact(
                                        title = "HEIGHT",
                                        checked = invertHeight,
                                        onCheckedChange = { invertHeight = it }
                                    )
                                },
                                right = {
                                    AxisSwitchCompact(
                                        title = "TURN",
                                        checked = invertTurn,
                                        onCheckedChange = { invertTurn = it }
                                    )
                                }
                            )
                        }

                        1 -> {
                            DiscoverTopicsButton(
                                discovering = discovering,
                                status = discoverStatus,
                                onClick = {
                                    if (addr.isBlank()) {
                                        discoverStatus = "IP REQUIRED"
                                    } else {
                                        discovering = true
                                        discoverStatus = "SCANNING $addr..."
                                        scope.launch {
                                            ros.disconnect()
                                            var connected = false
                                            ros.connect("ws://$addr") { connected = true }
                                            delay(5000)
                                            if (!connected) {
                                                discovering = false
                                                discoverStatus = "UNABLE TO CONNECT. CHECK IP:PORT."
                                            } else {
                                                ros.discoverTopics { res ->
                                                    discovering = false
                                                    res.onSuccess { disc ->
                                                        allDiscoveredTopics = disc.allTopics
                                                        cmdVelTopic = disc.cmdVelTopic
                                                        modeTopic = disc.modeTopic
                                                        batteryTopic = disc.batteryTopic
                                                        imuTopic = disc.imuTopic
                                                        odomTopic = disc.odomTopic
                                                        jointStateTopic = disc.jointStateTopic
                                                        discoverStatus = "SYNC COMPLETE"
                                                    }.onFailure {
                                                        discoverStatus = "SYNC FAILED"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )

                            TopicBindingDropdown(
                                title = "CMD VEL",
                                selected = cmdVelTopic,
                                options = buildTopicOptions(allDiscoveredTopics, listOf("geometry_msgs/msg/Twist")),
                                placeholder = "Select Topic...",
                                onSelected = { cmdVelTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "MODE TOPIC",
                                selected = modeTopic,
                                options = buildTopicOptions(allDiscoveredTopics, listOf("std_msgs/msg/String")),
                                placeholder = "Select Topic...",
                                onSelected = { modeTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "BATTERY",
                                selected = batteryTopic,
                                options = buildTopicOptions(
                                    allDiscoveredTopics,
                                    listOf("sensor_msgs/msg/BatteryState")
                                ),
                                placeholder = "Select Topic...",
                                onSelected = { batteryTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "IMU",
                                selected = imuTopic,
                                options = buildTopicOptions(allDiscoveredTopics, listOf("sensor_msgs/msg/Imu")),
                                placeholder = "Select Topic...",
                                onSelected = { imuTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "ODOM",
                                selected = odomTopic,
                                options = buildTopicOptions(allDiscoveredTopics, listOf("nav_msgs/msg/Odometry")),
                                placeholder = "Select Topic...",
                                onSelected = { odomTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "JOINT STATES",
                                selected = jointStateTopic,
                                options = buildTopicOptions(allDiscoveredTopics, listOf("sensor_msgs/msg/JointState")),
                                placeholder = "Select Topic...",
                                onSelected = { jointStateTopic = it }
                            )
                        }

                        2 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.padding(end = 6.dp),
                                    text = "ADD MODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudBlue,
                                    letterSpacing = 1.sp
                                )

                                IconButton(
                                    onClick = {
                                        modeToEditIndex = null
                                        showModeDialog = true
                                    },
                                    modifier = Modifier
                                        .background(HudBlue.copy(alpha = 0.1f), CircleShape)
                                        .border(1.dp, HudBlue.copy(alpha = 0.4f), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = HudBlue)
                                }
                            }

                            modes.forEachIndexed { index, mode ->
                                ModeEditorRow(
                                    mode = mode,
                                    onEdit = {
                                        modeToEditIndex = index
                                        showModeDialog = true
                                    },
                                    onDelete = { modeToDeleteIndex = index }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CyberButton(
                        onClick = {
                            if (isAdding && selectedTabOrStep > 0) {
                                selectedTabOrStep--
                            } else {
                                editingRobot = null
                                isAdding = false
                                selectedTabOrStep = 0
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                if (isAdding && selectedTabOrStep > 0) {
                                    R.drawable.back_button
                                } else {
                                    R.drawable.cancel_button
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }

                    val canGoNext =
                        if (selectedTabOrStep == 0) name.isNotBlank() && addr.isNotBlank() else true

                    CyberButton(
                        enabled = canGoNext,
                        onClick = {
                            if (isAdding && selectedTabOrStep < 2) {
                                selectedTabOrStep++
                                if (selectedTabOrStep > maxStepReached) {
                                    maxStepReached = selectedTabOrStep
                                }
                            } else {
                                val thumb =
                                    if (selectedThumbnailUri != null && selectedThumbnailUri.toString() != initial.thumbnailPath) {
                                        saveImageToInternalStorage(context, selectedThumbnailUri!!)
                                    } else {
                                        initial.thumbnailPath
                                    }

                                val currentOwnerUid = RobotManager.normalizeOwnerUid(FirebaseAuth.getInstance().currentUser?.uid)
                                val newConfig = RobotConfig(
                                    name = name,
                                    rosAddress = addr,
                                    videoUrl = url,
                                    thumbnailPath = thumb,
                                    cmdVelTopic = cmdVelTopic,
                                    modeTopic = modeTopic,
                                    batteryTopic = batteryTopic,
                                    imuTopic = imuTopic,
                                    odomTopic = odomTopic,
                                    jointStateTopic = jointStateTopic,
                                    modes = modes.toList(),
                                    enabledIndicators = initial.enabledIndicators,
                                    totalUptimeSeconds = initial.totalUptimeSeconds,
                                    totalDistanceMeters = initial.totalDistanceMeters,
                                    invertForwardBack = invertForwardBack,
                                    invertStrafe = invertStrafe,
                                    invertHeight = invertHeight,
                                    invertTurn = invertTurn,
                                    robotId = editingRobot?.robotId ?: initial.robotId,
                                    ownerUid = currentOwnerUid
                                )

                                onSave(editingRobot?.name, newConfig)

                                firestoreStatus = "Saving robot to cloud..."
                                pendingCloseAfterCloudSave = true

                                saveRobotConfigToFirestore(newConfig) { result ->
                                    firestoreStatus = result
                                    showCloudResultDialog = true
                                }


                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        val imgRes = when {
                            isAdding && selectedTabOrStep < 2 -> R.drawable.next_button
                            isAdding && selectedTabOrStep == 2 -> R.drawable.finish_button
                            else -> R.drawable.save_button
                        }

                        Image(
                            painter = painterResource(imgRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DiscoverTopicsButton(
    discovering: Boolean,
    status: String?,
    onClick: () -> Unit
) {
    val rotation = rememberInfiniteTransition(label = "sync")
    val angle by rotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HudBlue.copy(alpha = 0.05f))
            .border(1.dp, HudBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(enabled = !discovering, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = if (discovering) HudBlue else HudText.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (discovering) angle else 0f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AUTO DISCOVER TOPICS",
                    color = HudBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (!status.isNullOrBlank()) {
                    Text(
                        text = status.uppercase(),
                        color = HudText.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}

@Composable
fun StartMenuScreen(
    ros: RosbridgeClient,
    savedRobots: List<RobotConfig>,
    terminalText: String,
    isSignedIn: Boolean,
    signedInLabel: String,
    onLaunchGamepad: (RobotConfig) -> Unit,
    onLaunchSetup: () -> Unit,
    onOpenAccount: () -> Unit
) {
    var showRobotSelectDialog by remember { mutableStateOf(false) }

    var showNoRobotWarning by remember { mutableStateOf(false) }

    CyberDialog(
        show = showNoRobotWarning,
        title = "PILOT AUTHORIZATION",
        confirmText = "GO TO SETUP",
        onConfirm = {
            showNoRobotWarning = false
            onLaunchSetup()
        },
        onDismiss = { showNoRobotWarning = false }
    ) {
        Text(
            "No active robot links detected. Navigate to Setup to initialize a new hardware profile.",
            color = HudText,
            fontSize = 14.sp
        )
    }

    if (showRobotSelectDialog) {
        RobotSelectionDialog(
            savedRobots = savedRobots,
            onDismiss = { showRobotSelectDialog = false },
            onSelect = { robot ->
                showRobotSelectDialog = false
                onLaunchGamepad(robot)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_main),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Profile Button removed, now persistent in AppNavigation

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1.5f)
            ) {
                Image(
                    painter = painterResource(R.drawable.terminal_box),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 58.dp, top = 28.dp, end = 54.dp, bottom = 24.dp)
                ) {
                    Text(
                        text = terminalText,
                        color = TerminalCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            MainMenuPanel {
                CyberButton(
                    onClick = {
                        if (savedRobots.isEmpty()) {
                            showNoRobotWarning = true
                        } else {
                            showRobotSelectDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.launch_controller),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (savedRobots.isEmpty()) 0.5f else 1f),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CyberButton(
                    onClick = onLaunchSetup,
                    modifier = Modifier.fillMaxWidth(0.75f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.robot_setup),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
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
        title = "Robot Selection",
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
                        .background(if (isSelected) HudBlue.copy(alpha = 0.1f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) HudBlue else HudBorder.copy(alpha = 0.3f),
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
                            selectedColor = HudBlue,
                            unselectedColor = HudText.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = robot.name,
                            color = if (isSelected) HudBlue else HudText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (selected?.name?.contains("ROSbot", ignoreCase = true) == true) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = HudBlue.copy(alpha = 0.05f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, HudBlue.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = HudBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NOTE: This is a demo profile to test the UI. Real-time hardware telemetry and video streaming will not be available until a live ROS bridge is configured.",
                        color = HudText.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}


// --- Helper / Utility functions ---

data class TopicDropdownItem(
    val binding: TopicBinding? = null,
    val label: String? = null,
    val isHeader: Boolean = false
)



@Composable
fun HudTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        placeholder = {
            Text(
                text = "Enter ${label.lowercase()}...",
                color = HudText.copy(alpha = 0.3f)
            )
        },
        textStyle = TextStyle(color = HudText, fontSize = 14.sp),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        minLines = 1,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HudBlue,
            unfocusedBorderColor = HudText.copy(alpha = 0.4f),
            focusedTextColor = HudText,
            unfocusedTextColor = HudText,
            unfocusedLabelColor = HudBlue,
            focusedLabelColor = HudBlue,
            disabledLabelColor = HudBlue
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun AxisSwitchRowDual(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    Surface(
        color = HudText.copy(alpha = 0.03f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, HudText.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                left()
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(HudText.copy(alpha = 0.1f))
            )

            Box(modifier = Modifier.weight(1f)) {
                right()
            }
        }
    }
}

@Composable
fun AxisSwitchCompact(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = HudText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 11.sp
            )

            Text(
                text = if (checked) "INVERTED" else "NORMAL",
                color = if (checked) HudBlue else HudText.copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 9.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HudBlue,
                checkedTrackColor = HudBlue.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.DarkGray,
                uncheckedTrackColor = Color.Black.copy(alpha = 0.4f),
                uncheckedBorderColor = HudText.copy(alpha = 0.2f),
                checkedBorderColor = HudText.copy(alpha = 0.2f)
            ),
            modifier = Modifier.scale(0.6f)
        )
    }
}

@Composable
fun AxisSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) // 👈 hard cap height (this is key)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = HudText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 12.sp // 👈 tighter text
            )

            Text(
                text = if (checked) "INVERTED" else "NORMAL",
                color = if (checked) HudBlue else HudText.copy(alpha = 0.6f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 9.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HudBlue,
                checkedTrackColor = HudBlue.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.DarkGray,
                uncheckedTrackColor = Color.Black.copy(alpha = 0.4f),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.scale(0.6f)
        )
    }
}
@Composable
fun MainMenuPanel(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),   // was aspectRatio(1.5f)
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicBindingDropdown(
    title: String,
    selected: TopicBinding?,
    options: List<TopicDropdownItem>,
    placeholder: String,
    onSelected: (TopicBinding?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(title, fontSize = 10.sp) },
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = HudText.copy(alpha = 0.3f)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(60.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = HudText.copy(alpha = 0.4f),
                focusedBorderColor = HudBlue,
                focusedTextColor = HudText,
                unfocusedTextColor = HudText,
                unfocusedLabelColor = HudBlue,
                focusedLabelColor = HudBlue
            ),
            shape = RoundedCornerShape(8.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(HudSurface)
                .border(1.dp, HudBorder, RoundedCornerShape(4.dp))
        ) {
            DropdownMenuItem(
                text = {
                    Text("Not set", color = HudText.copy(alpha = 0.6f))
                },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
                colors = MenuDefaults.itemColors(textColor = HudText)
            )

            options.forEach { item ->
                if (item.isHeader) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = HudBlue.copy(alpha = 0.25f)
                    )
                    Text(
                        text = item.label ?: "",
                        color = HudBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                } else {
                    val option = item.binding ?: return@forEach

                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = option.name,
                                    color = HudText,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = option.type,
                                    color = HudBlue.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(textColor = HudText)
                    )
                }
            }
        }
    }
}
@Composable
fun ModeEditorRow(
    mode: RobotMode,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = HudText.copy(alpha = 0.0f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HudText.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.label,
                    color = HudText,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = HudText)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color.Red)
            }
        }
    }
}

@Composable
fun RobotListItem(
    robot: RobotConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = HudText.copy(alpha = 0.0f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HudText.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HudBackground)
            ) {
                if (robot.thumbnailPath == "demo_thumb") {
                    Image(
                        painter = painterResource(id = R.drawable.jax_demo_icon),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                } else if (robot.thumbnailPath != null) {
                    AsyncImage(
                        model = File(robot.thumbnailPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                        tint = HudText.copy(alpha = 0.2f)
                    )
                }
            }
            Text(
                text = robot.name,
                color = HudText,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = HudText.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MjpegWebView(
    url: String,
    onLoadingStateChanged: (loaded: Boolean, error: String?) -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setBackgroundColor(android.graphics.Color.BLACK)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingStateChanged(true, null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onLoadingStateChanged(false, error?.description?.toString())
                            view?.visibility = android.view.View.INVISIBLE
                        }
                    }
                }
                settings.javaScriptEnabled = true
                setOnTouchListener { _, _ ->
                    onUserInteraction()
                    false
                }
                loadUrl(url)
            }
        },
        update = {
            it.visibility = android.view.View.VISIBLE
            it.loadUrl(url)
        },
        modifier = modifier
    )
}

@Composable
fun VideoFeedContainer(
    modifier: Modifier = Modifier,
    hatchOpen: Boolean,
    errorText: String?,
    videoUrl: String,
    videoContent: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.dp, HudBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        videoContent()

        if (hatchOpen) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val primaryMsg = if (videoUrl.isNotBlank()) "SIGNAL LINK ERROR" else "NO VIDEO FEED CONFIGURED"
                val secondaryMsg = if (videoUrl.isNotBlank()) "CHECK NETWORK STATUS" else "SERVER URL REQUIRED"

                Icon(
                    imageVector = if (videoUrl.isNotBlank()) {
                        Icons.Default.SignalWifiStatusbarConnectedNoInternet4
                    } else {
                        Icons.Default.VideocamOff
                    },
                    contentDescription = null,
                    tint = HudBlue.copy(alpha = 0.5f),
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = primaryMsg,
                    color = HudBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = secondaryMsg,
                    color = HudBlue.copy(alpha = 0.6f),
                    fontSize = 8.sp
                )
                if (videoUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = videoUrl,
                        color = HudText.copy(alpha = 0.4f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }

        HatchOverlay(
            modifier = Modifier.matchParentSize(),
            isOpen = hatchOpen
        )

        if (!errorText.isNullOrBlank() && !hatchOpen) {
            Text(
                text = "VIDEO SYSTEM OFFLINE",
                color = HudBlue,
                modifier = Modifier.align(Alignment.Center),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun HatchOverlay(
    modifier: Modifier = Modifier,
    isOpen: Boolean
) {
    val openFraction by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "hatch_anim"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val halfWidth = maxWidth / 2
        val translationAmount = openFraction * (constraints.maxWidth.toFloat() / 2f)

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(halfWidth)
                .align(Alignment.CenterStart)
                .graphicsLayer { translationX = -translationAmount }
        ) {
            Image(
                painter = painterResource(id = R.drawable.hatch_door_left),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(halfWidth)
                .align(Alignment.CenterEnd)
                .graphicsLayer { translationX = translationAmount }
        ) {
            Image(
                painter = painterResource(id = R.drawable.hatch_door_right),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}

@Composable
fun SettingsDialog(
    savedRobots: List<RobotConfig>,
    currentRobot: RobotConfig,
    initialHaptics: Boolean,
    activeIndicators: Set<HudIndicator>,
    onToggleIndicator: (HudIndicator) -> Unit,
    onDismiss: () -> Unit,
    onSave: (RobotConfig, Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onBackToMenu: () -> Unit
) {
    var haptics by remember { mutableStateOf(initialHaptics) }

    CyberDialog(
        show = true,
        title = "CONTROLLER SETTINGS",
        confirmText = "APPLY ▶",
        onConfirm = { onSave(currentRobot, haptics) },
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsRockerRow(
                label = "ENABLE HAPTIC FEEDBACK",
                checked = haptics,
                onToggle = { haptics = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val leds = listOf(HudIndicator.ROS_LINK, HudIndicator.MOTORS, HudIndicator.IMU, HudIndicator.CAMERA)
                    leds.forEach { indicator ->
                        IndicatorRockerRow(indicator, activeIndicators, onToggleIndicator)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val telemetry = listOf(HudIndicator.BATTERY, HudIndicator.CPU)
                    telemetry.forEach { indicator ->
                        IndicatorRockerRow(indicator, activeIndicators, onToggleIndicator)
                    }
                }
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
        border = BorderStroke(1.dp, HudBlue.copy(alpha = 0.1f))
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
                color = if (checked) HudText else Color.Gray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Switch(
                checked = checked,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = HudBlue,
                    checkedTrackColor = HudBlue.copy(alpha = 0.3f),
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
        border = BorderStroke(1.dp, HudBlue.copy(alpha = 0.1f))
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
                color = if (isEnabled) HudText else Color.Gray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Switch(
                checked = isEnabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = HudBlue,
                    checkedTrackColor = HudBlue.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.DarkGray,
                    uncheckedTrackColor = Color.Black.copy(alpha = 0.4f),
                    uncheckedBorderColor = HudText.copy(alpha = 0.2f),
                    checkedBorderColor = HudText.copy(alpha = 0.2f)
                ),
                modifier = Modifier.scale(0.6f)
            )
        }
    }
}

@Composable
fun ModeEditDialog(
    initialMode: RobotMode?,
    onDismiss: () -> Unit,
    onSave: (RobotMode) -> Unit
) {
    var label by remember { mutableStateOf(initialMode?.label ?: "") }
    var cmd by remember { mutableStateOf(initialMode?.command ?: "") }

    CyberDialog(
        show = true,
        title = if (initialMode == null) "NEW MODE CONFIG" else "EDIT MODE CONFIG",
        confirmText = "SAVE ▶",
        onConfirm = {
            if (label.isNotBlank() && cmd.isNotBlank()) {
                onSave(RobotMode(label, cmd))
            }
        },
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "ASSIGN LABEL AND COMMAND STRING",
                color = HudBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            HudTextField(value = label, onValueChange = { label = it }, label = "LABEL (e.g. WALK)")
            HudTextField(value = cmd, onValueChange = { cmd = it }, label = "COMMAND (e.g. walk)")
        }
    }
}
@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "Robot List")
@Composable
fun PreviewRobotSetupListScreen() {
    JaxGamepadTheme {
        RobotSetupScreen(
            ros = RosbridgeClient(),
            savedRobots = listOf(
                RobotConfig(
                    name = "Jax",
                    rosAddress = "192.168.1.154:9090",
                    videoUrl = "http://192.168.1.154:8080/stream?topic=/image_raw",
                    thumbnailPath = "demo_thumb",
                    cmdVelTopic = TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
                    modeTopic = TopicBinding("/jax_mode", "std_msgs/String"),
                    jointStateTopic = TopicBinding("/joint_states", "sensor_msgs/JointState"),
                    modes = listOf(
                        RobotMode("STAND", "stand"),
                        RobotMode("WALK", "walk"),
                        RobotMode("SIT", "sit")
                    )
                )
            ),
            onSave = { _, _ -> },
            onDelete = {},
            onBack = {},
            onOpenAccount = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "New Robot - Step 1")
@Composable
fun PreviewNewRobotStep1() {
    JaxGamepadTheme {
        RobotSetupScreen(
            ros = RosbridgeClient(),
            savedRobots = listOf(
                RobotConfig(
                    name = "ROSbot (Demo)",
                    rosAddress = "192.168.1.XX:9090",
                    videoUrl = "",
                    thumbnailPath = "demo_thumb",
                    cmdVelTopic = TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
                    modeTopic = TopicBinding("/jax_mode", "std_msgs/String"),
                    jointStateTopic = TopicBinding("/joint_states", "sensor_msgs/JointState"),
                    modes = listOf(
                        RobotMode("STAND", "stand"),
                        RobotMode("WALK", "walk")
                    )
                )
            ),
            onSave = { _, _ -> },
            onDelete = {},
            onBack = {},
            onOpenAccount = {},
            initialEditingRobot = RobotConfig(
                name = "",
                rosAddress = "",
                videoUrl = "",
                thumbnailPath = null,
                modes = emptyList()
            ),
            initialIsAdding = true,
            initialSelectedTabOrStep = 0
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "New Robot - Topics")
@Composable
fun PreviewNewRobotStep2Topics() {
    JaxGamepadTheme {
        RobotSetupScreen(
            ros = RosbridgeClient(),
            savedRobots = listOf(
                RobotConfig(
                    name = "ROSbot (Demo)",
                    rosAddress = "192.168.1.XX:9090",
                    videoUrl = "",
                    thumbnailPath = "demo_thumb",
                    cmdVelTopic = TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
                    modeTopic = TopicBinding("/jax_mode", "std_msgs/String"),
                    jointStateTopic = TopicBinding("/joint_states", "sensor_msgs/JointState"),
                    modes = listOf(
                        RobotMode("STAND", "stand"),
                        RobotMode("WALK", "walk")
                    )
                )
            ),
            onSave = { _, _ -> },
            onDelete = {},
            onBack = {},
            onOpenAccount = {},
            initialEditingRobot = RobotConfig(
                name = "Jax",
                rosAddress = "192.168.1.154:9090",
                videoUrl = "http://192.168.1.154:8080/stream?topic=/image_raw",
                thumbnailPath = null,
                modes = emptyList()
            ),
            initialIsAdding = true,
            initialSelectedTabOrStep = 1
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "New Robot - Modes")
@Composable
fun PreviewNewRobotStep3Modes() {
    JaxGamepadTheme {
        RobotSetupScreen(
            ros = RosbridgeClient(),
            savedRobots = listOf(
                RobotConfig(
                    name = "ROSbot (Demo)",
                    rosAddress = "192.168.1.XX:9090",
                    videoUrl = "",
                    thumbnailPath = "demo_thumb",
                    cmdVelTopic = TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
                    modeTopic = TopicBinding("/jax_mode", "std_msgs/String"),
                    jointStateTopic = TopicBinding("/joint_states", "sensor_msgs/JointState"),
                    modes = listOf(
                        RobotMode("STAND", "stand"),
                        RobotMode("WALK", "walk")
                    )
                )
            ),
            onSave = { _, _ -> },
            onDelete = {},
            onBack = {},
            onOpenAccount = {},
            initialEditingRobot = RobotConfig(
                name = "Jax",
                rosAddress = "192.168.1.154:9090",
                videoUrl = "http://192.168.1.154:8080/stream?topic=/image_raw",
                thumbnailPath = null,
                modes = listOf(
                    RobotMode("STAND", "stand"),
                    RobotMode("WALK", "walk"),
                    RobotMode("SIT", "sit")
                )
            ),
            initialIsAdding = true,
            initialSelectedTabOrStep = 2
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "Edit Robot")
@Composable
fun PreviewEditRobotScreen() {
    JaxGamepadTheme {
        RobotSetupScreen(
            ros = RosbridgeClient(),
            savedRobots = listOf(
                RobotConfig(
                    name = "Jax",
                    rosAddress = "192.168.1.154:9090",
                    videoUrl = "http://192.168.1.154:8080/stream?topic=/image_raw",
                    thumbnailPath = "demo_thumb",
                    cmdVelTopic = TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
                    modeTopic = TopicBinding("/jax_mode", "std_msgs/String"),
                    jointStateTopic = TopicBinding("/joint_states", "sensor_msgs/JointState"),
                    modes = listOf(
                        RobotMode("STAND", "stand"),
                        RobotMode("WALK", "walk"),
                        RobotMode("SIT", "sit"),
                        RobotMode("LAY", "lay")
                    ),
                    invertForwardBack = false,
                    invertStrafe = true,
                    invertHeight = false,
                    invertTurn = true
                )
            ),
            onSave = { _, _ -> },
            onDelete = {},
            onBack = {},
            onOpenAccount = {},
            initialEditingRobot = RobotConfig(
                name = "Jax",
                rosAddress = "192.168.1.154:9090",
                videoUrl = "http://192.168.1.154:8080/stream?topic=/image_raw",
                thumbnailPath = "demo_thumb",
                cmdVelTopic = TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
                modeTopic = TopicBinding("/jax_mode", "std_msgs/String"),
                jointStateTopic = TopicBinding("/joint_states", "sensor_msgs/JointState"),
                modes = listOf(
                    RobotMode("STAND", "stand"),
                    RobotMode("WALK", "walk"),
                    RobotMode("SIT", "sit"),
                    RobotMode("LAY", "lay")
                ),
                invertForwardBack = false,
                invertStrafe = true,
                invertHeight = false,
                invertTurn = true
            ),
            initialIsAdding = false,
            initialSelectedTabOrStep = 0
        )
    }
}

@Preview(showBackground = true, widthDp = 915, heightDp = 412)
@Composable
fun PreviewJaxDriverScreen() {
    JaxGamepadTheme {
        JaxDriverScreen(
            ros = RosbridgeClient(),
            currentRobot = RobotConfig(
                name = "Jax",
                rosAddress = "",
                videoUrl = "",
                thumbnailPath = "demo_thumb",
                cmdVelTopic = TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
                modeTopic = TopicBinding("/jax_mode", "std_msgs/String"),
                jointStateTopic = TopicBinding("/joint_states", "sensor_msgs/JointState"),
                modes = listOf(
                    RobotMode("STAND", "stand"),
                    RobotMode("WALK", "walk"),
                    RobotMode("SIT", "sit"),
                    RobotMode("LAY", "lay")
                ),
                enabledIndicators = listOf("LINK", "MOTORS", "IMU", "SAFE")
            ),
            savedRobots = emptyList(),
            hapticsEnabled = true,
            onRobotChange = {},
            onHapticsChange = {},
            reHideSystemBars = {},
            onBackToMenu = {},
            networkInfo = "WIFI_CONNECTED" to "192.168.1.154"
        )
    }
}

@Preview(
    name = "Main Menu - Terminal Booted",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait"
)
@Composable
fun StartMenuScreenPreview() {
    val mockTerminalText = """> BOOTING_ROS_CONTROLLER...
|> 
|> LINK: WIFI_CONNECTED
|> ADDR: 192.168.1.15
|> 
|> STATUS: SYSTEM_READY_"""

    val sampleRobots = listOf(
        RobotConfig(
            name = "Jax-1",
            rosAddress = "192.168.1.15",
            videoUrl = "http://192.168.1.15:8080/stream",
            thumbnailPath = "demo_thumb"
        )
    )

    JaxGamepadTheme {
        StartMenuScreen(
            ros = RosbridgeClient(),
            savedRobots = sampleRobots,
            terminalText = mockTerminalText,
            isSignedIn = false,
            signedInLabel = "",
            onLaunchGamepad = {},
            onLaunchSetup = {},
            onOpenAccount = {}
        )
    }
}