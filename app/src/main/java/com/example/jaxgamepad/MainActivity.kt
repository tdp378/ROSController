package com.example.jaxgamepad

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.jaxgamepad.ui.screens.HudIndicator
import com.example.jaxgamepad.ui.screens.JaxHudScreen
import com.example.jaxgamepad.ui.theme.JaxGamepadTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import com.example.jaxgamepad.ui.screens.JaxDriverScreen
import com.example.jaxgamepad.ui.screens.RobotSetupScreen
import com.example.jaxgamepad.ui.screens.StartMenuScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.jaxgamepad.ui.theme.MyColors



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

enum class Screen { Menu, Gamepad, RobotSetup }


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
                    .background(MyColors.HudBlue.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, MyColors.HudBlue.copy(alpha = 0.4f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenHelp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Help,
                    contentDescription = "Help",
                    tint = MyColors.HudBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Profile Icon on the Right
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MyColors.HudBlue.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, MyColors.HudBlue.copy(alpha = 0.4f), CircleShape)
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
                    tint = MyColors.HudBlue,
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

    val portraitScreens = setOf(Screen.Menu, Screen.RobotSetup)

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
                val withDemo = merged.ifEmpty { loadRobotsForOwner(ownerUid) }
                syncRobotsToFirestoreForSignedInUser(withDemo)
                savedRobots = withDemo
                currentRobot = withDemo.firstOrNull { it.robotId == currentRobot.robotId }
                    ?: withDemo.firstOrNull()
                            ?: buildDemoRobot(ownerUid)
            }
        )
    }

    var showGlobalHelp by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }

    val googleAuthManager = remember { GoogleAuthManager(context) }
    val scope = rememberCoroutineScope()

    if (showGlobalHelp) {
        HelpDialog(show = true, onDismiss = { showGlobalHelp = false })
    }

    AccountStatusDialog(
        show = showAccountDialog,
        user = signedInUser,
        onDismiss = { showAccountDialog = false },
        onGoogleLogin = {
            scope.launch {
                val result = googleAuthManager.signInWithGoogle()
                result.onSuccess {
                    showAccountDialog = false
                }.onFailure { e ->
                    Log.e("GOOGLE_AUTH", "Sign in failed", e)
                }
            }
        },
        onSignOut = {
            FirebaseAuth.getInstance().signOut()
            showAccountDialog = false
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        if (currentScreen != Screen.Gamepad) {
            GlobalTopBar(
                onOpenAccount = {
                    showAccountDialog = true
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
                        showAccountDialog = true
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
                        showAccountDialog = true
                    }
                )
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


// --- Helper / Utility functions ---




@Composable
fun MyColors.HudTextField(value: String, onValueChange: (String) -> Unit, label: String) {
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
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        minLines = 1,
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