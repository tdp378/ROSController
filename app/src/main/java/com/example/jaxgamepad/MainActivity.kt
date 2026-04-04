package com.example.jaxgamepad

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.example.jaxgamepad.ui.JaxHudScreen
import com.example.jaxgamepad.ui.theme.JaxGamepadTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// --- UI Constants ---
val HudBlue = Color(0xFF0e7edb)
val HudBorder = Color(0xFF1A3A4A)
val HudBackground = Color(0xFF050B10)
val HudSurface = Color(0xFF0D1B26)
val HudText = Color(0xFFE6EEF5)
val Black = Color(0xFF000000)

@Composable
fun CyberDialog(
    show: Boolean,
    title: String,
    confirmText: String = "LAUNCH ▶",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!show) return

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = HudBlue,
                    spotColor = Color(0xFF008CFF)
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(HudBlue, Color(0xFF008CFF), HudBlue)),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF020617).copy(alpha = 0.95f),
                            Color(0xFF020617).copy(alpha = 0.85f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = title,
                    color = HudText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                content()

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Not Now", color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    CyberButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .height(45.dp)
                            .width(130.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, HudBlue, RoundedCornerShape(10.dp))
                                .background(HudBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmText,
                                color = HudBlue,
                                fontWeight = FontWeight.Bold
                            )
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

enum class Screen { Menu, Gamepad, RobotSetup }

@Composable
fun AppNavigation(reHideSystemBars: () -> Unit) {
    val context = LocalContext.current
    val robotManager = remember { RobotManager(context) }

    var savedRobots by remember {
        val loaded = robotManager.loadRobots()
        val cleaned = if (loaded.isEmpty() || loaded.any { it.name.lowercase() == "jax-1" }) {
            val sample = listOf(RobotConfig(
                name = "ROSbot (Demo)",
                rosAddress = "192.168.1.XX",
                videoUrl = "http://192.168.1.XX:8080/stream?topic=/camera/image_raw",
                thumbnailPath = "demo_thumb",
                modes = listOf(
                    RobotMode("STAND", "stand"),
                    RobotMode("WALK", "walk"),
                    RobotMode("LAY", "lay"),
                    RobotMode("SHAKE", "shake"),
                    RobotMode("SIT", "sit"),
                    RobotMode("WAVE", "wave"),
                )
            ))
            robotManager.saveRobots(sample)
            sample
        } else loaded
        mutableStateOf(cleaned)
    }

    var currentScreen by remember { mutableStateOf(Screen.Menu) }
    var currentRobot by remember { mutableStateOf(savedRobots.firstOrNull() ?: RobotConfig("ROSbot (Demo)", "", "")) }
    var hapticsEnabled by remember { mutableStateOf(true) }

    val ros = remember { RosbridgeClient() }
    val activity = LocalContext.current as? ComponentActivity

    LaunchedEffect(currentScreen) {
        activity?.let {
            if (currentScreen == Screen.RobotSetup || currentScreen == Screen.Menu) {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                WindowInsetsControllerCompat(it.window, it.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            } else {
                it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                reHideSystemBars()
            }
        }
    }

    when (currentScreen) {
        Screen.Menu -> StartMenuScreen(
            ros = ros,
            savedRobots = savedRobots,
            onLaunchGamepad = { robot ->
                currentRobot = robot
                currentScreen = Screen.Gamepad
            },
            onLaunchSetup = { currentScreen = Screen.RobotSetup }
        )
        Screen.Gamepad -> JaxDriverScreen(
            ros = ros,
            currentRobot = currentRobot,
            savedRobots = savedRobots,
            hapticsEnabled = hapticsEnabled,
            onRobotChange = { newRobot -> currentRobot = newRobot },
            onHapticsChange = { hapticsEnabled = it },
            reHideSystemBars = reHideSystemBars,
            onBackToMenu = { currentScreen = Screen.Menu }
        )
        Screen.RobotSetup -> RobotSetupScreen(
            ros = ros,
            savedRobots = savedRobots,
            onSave = { oldName, newRobot ->
                val list = savedRobots.toMutableList()
                val index = list.indexOfFirst { it.name == oldName }
                if (index != -1) list[index] = newRobot else list.add(newRobot)
                savedRobots = list
                robotManager.saveRobots(list)
                if (currentRobot.name == oldName) currentRobot = newRobot
            },
            onDelete = { robot ->
                val list = savedRobots.filter { it.name != robot.name }
                savedRobots = list
                robotManager.saveRobots(list)
                if (list.isNotEmpty()) {
                    currentRobot = list.first()
                }
            },
            onBack = { currentScreen = Screen.Menu }
        )
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
    onBackToMenu: () -> Unit
) {
    var moveX by remember { mutableStateOf(0.0) }
    var moveY by remember { mutableStateOf(0.0) }
    var turnZ by remember { mutableStateOf(0.0) }
    var showSettings by remember { mutableStateOf(false) }

    var videoLoadedManually by remember(currentRobot.name) { mutableStateOf(false) }
    var videoError by remember(currentRobot.videoUrl) { mutableStateOf<String?>(null) }
    var videoButtonActive by remember(currentRobot.videoUrl) { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(currentRobot.rosAddress) {
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

    DisposableEffect(ros.isConnected, moveX, moveY, turnZ, currentRobot) {
        val publishJob: Job? = if (ros.isConnected) {
            scope.launch {
                while (true) {
                    ros.publishCmdVel(currentRobot, moveY, moveX, turnZ)
                    delay(75)
                }
            }
        } else null
        onDispose { publishJob?.cancel() }
    }

    if (showSettings) {
        SettingsDialog(
            savedRobots = savedRobots,
            currentRobot = currentRobot,
            initialHaptics = hapticsEnabled,
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

    JaxHudScreen(
        robotName = currentRobot.name,
        batteryPercent = ros.lastBatteryPercent ?: 0,
        selectedMode = ros.lastModeText ?: currentRobot.modes.firstOrNull()?.command ?: "walk",
        modes = currentRobot.modes,
        leftJoystickValue = moveX.toFloat() to moveY.toFloat(),
        rightJoystickValue = turnZ.toFloat() to 0f,
        videoActive = videoButtonActive,
        hapticsEnabled = hapticsEnabled,
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
        onModeSelected = { mode -> ros.publishMode(currentRobot, mode) },
        onSettingsClick = { showSettings = true },
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
                        onLoadingStateChanged = { loaded, error ->
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
    initialEditingRobot: RobotConfig? = null,
    initialIsAdding: Boolean = false,
    initialSelectedTabOrStep: Int = 0
) {
    val context = LocalContext.current
    var editingRobot by remember(initialEditingRobot) { mutableStateOf(initialEditingRobot) }
    var isAdding by remember(initialIsAdding) { mutableStateOf(initialIsAdding) }
    var robotToDelete by remember { mutableStateOf<RobotConfig?>(null) }
    var showRosRequirements by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var selectedTabOrStep by remember { mutableIntStateOf(initialSelectedTabOrStep) }
    var maxStepReached by remember(isAdding) { mutableIntStateOf(initialSelectedTabOrStep) }

    var showWelcomeDialog by remember(savedRobots) {
        mutableStateOf(savedRobots.size == 1 && savedRobots.first().name.contains("ROSbot"))
    }

    CyberDialog(
        show = showWelcomeDialog && editingRobot == null && !isAdding,
        title = "SYSTEM NOTIFICATION",
        confirmText = "ADD NEW ▶",
        onConfirm = {
            showWelcomeDialog = false
            showRosRequirements = true
        },
        onDismiss = { showWelcomeDialog = false }
    ) {
        Text(
            "We have provided you with a sample robot (ROSbot) to test the UI. Configure your own robot to realize the full functionality of the controller.",
            color = HudText,
            fontSize = 14.sp
        )
    }

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
            Text("Hardware requires the following environment:", color = HudBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("• ROS2 Humble or later recommended", color = HudText, fontSize = 13.sp)
            Text("• Rosbridge Suite (WebSocket) installed", color = HudText, fontSize = 13.sp)
            Text("• Network visibility to Robot IP:9090", color = HudText, fontSize = 13.sp)
            Text("• MJPEG stream for Video Support", color = HudText, fontSize = 13.sp)
        }
    }

    if (robotToDelete != null) {
        AlertDialog(
            onDismissRequest = { robotToDelete = null },
            title = {
                Text(
                    text = "DELETE ROBOT",
                    color = HudBlue,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${robotToDelete?.name}'?",
                    color = HudText
                )
            },
            containerColor = HudBackground,
            confirmButton = {
                TextButton(
                    onClick = {
                        robotToDelete?.let { onDelete(it) }
                        robotToDelete = null
                    }
                ) {
                    Text("DELETE", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { robotToDelete = null }) {
                    Text("CANCEL", color = HudText.copy(alpha = 0.6f))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBackground)
            .statusBarsPadding()
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.height(25.dp))
                Image(
                    painter = painterResource(id = R.drawable.saved_robots),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(modifier = Modifier.padding(end = 6.dp),
                        text = "ADD NEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudBlue,
                        letterSpacing = 1.sp
                    )

                    IconButton(
                        onClick = { showRosRequirements = true },
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
            var selectedThumbnailUri by remember(editingRobot) {
                mutableStateOf<Uri?>(if (initial.thumbnailPath != null && initial.thumbnailPath != "demo_thumb") Uri.fromFile(File(initial.thumbnailPath)) else null)
            }

            var allDiscoveredTopics by remember { mutableStateOf<List<RosTopicInfo>>(emptyList()) }
            var cmdVelTopic by remember(editingRobot) { mutableStateOf(initial.cmdVelTopic) }
            var modeTopic by remember(editingRobot) { mutableStateOf(initial.modeTopic) }
            var batteryTopic by remember(editingRobot) { mutableStateOf(initial.batteryTopic) }
            var imuTopic by remember(editingRobot) { mutableStateOf(initial.imuTopic) }
            var odomTopic by remember(editingRobot) { mutableStateOf(initial.odomTopic) }
            var jointStateTopic by remember(editingRobot) { mutableStateOf(initial.jointStateTopic) }

            val modes = remember(editingRobot) { mutableStateListOf<RobotMode>().apply { addAll(initial.modes) } }
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
                        if (modeToEditIndex != null) modes[modeToEditIndex!!] = updatedMode
                        else if (modes.none { it.command.equals(updatedMode.command, ignoreCase = true) }) modes.add(updatedMode)
                        showModeDialog = false
                        modeToEditIndex = null
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.height(25.dp))
                Image(
                    painter = painterResource(
                        id = if (isAdding) R.drawable.new_robot else R.drawable.saved_robots
                    ),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )

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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTabOrStep) {
                        0 -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
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
                            }
                            HudTextField(value = name, onValueChange = { name = it }, label = "ROBOT NAME *")
                            HudTextField(value = addr, onValueChange = { addr = it }, label = "ROS BRIDGE IP (IP:PORT) *")
                            HudTextField(value = url, onValueChange = { url = it }, label = "VIDEO SERVER URL (OPTIONAL)")
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
                                options = buildTopicOptions(allDiscoveredTopics, listOf("geometry_msgs/Twist")),
                                placeholder = "Select Topic...",
                                onSelected = { cmdVelTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "MODE TOPIC",
                                selected = modeTopic,
                                options = buildTopicOptions(allDiscoveredTopics, listOf("std_msgs/String")),
                                placeholder = "Select Topic...",
                                onSelected = { modeTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "BATTERY",
                                selected = batteryTopic,
                                options = buildTopicOptions(allDiscoveredTopics, listOf("sensor_msgs/BatteryState")),
                                placeholder = "Select Topic...",
                                onSelected = { batteryTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "IMU",
                                selected = imuTopic,
                                options = buildTopicOptions(allDiscoveredTopics, listOf("sensor_msgs/Imu")),
                                placeholder = "Select Topic...",
                                onSelected = { imuTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "ODOM",
                                selected = odomTopic,
                                options = buildTopicOptions(allDiscoveredTopics, listOf("nav_msgs/Odometry")),
                                placeholder = "Select Topic...",
                                onSelected = { odomTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "JOINT STATES",
                                selected = jointStateTopic,
                                options = buildTopicOptions(allDiscoveredTopics, listOf("sensor_msgs/JointState")),
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

                                Text(modifier = Modifier.padding(end = 6.dp),
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
                                    onDelete = { modes.removeAt(index) }
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
                                if (isAdding && selectedTabOrStep > 0) R.drawable.back_button
                                else R.drawable.cancel_button
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }

                    val canGoNext = if (selectedTabOrStep == 0) name.isNotBlank() && addr.isNotBlank() else true
                    CyberButton(
                        enabled = canGoNext,
                        onClick = {
                            if (isAdding && selectedTabOrStep < 2) {
                                selectedTabOrStep++
                                if (selectedTabOrStep > maxStepReached) {
                                    maxStepReached = selectedTabOrStep
                                }
                            } else {
                                val thumb = if (selectedThumbnailUri != null && selectedThumbnailUri.toString() != initial.thumbnailPath) {
                                    saveImageToInternalStorage(context, selectedThumbnailUri!!)
                                } else initial.thumbnailPath

                                val newConfig = RobotConfig(
                                    name = name,
                                    rosAddress = addr,
                                    videoUrl = url,
                                    thumbnailPath = thumb,
                                    cmdVelTopic = cmdVelTopic ?: TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
                                    modeTopic = modeTopic ?: TopicBinding("/jax_mode", "std_msgs/String"),
                                    batteryTopic = batteryTopic,
                                    imuTopic = imuTopic,
                                    odomTopic = odomTopic,
                                    jointStateTopic = jointStateTopic,
                                    modes = modes.toList()
                                )
                                onSave(editingRobot?.name, newConfig)
                                editingRobot = null
                                isAdding = false
                                selectedTabOrStep = 0
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
    onLaunchGamepad: (RobotConfig) -> Unit,
    onLaunchSetup: () -> Unit
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.52f))

            MainMenuPanel {
                CyberButton(
                    onClick = {
                        if (savedRobots.isEmpty()) showNoRobotWarning = true
                        else showRobotSelectDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(.85f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.launch_controller),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().alpha(if (savedRobots.isEmpty()) 0.5f else 1f),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                CyberButton(
                    onClick = onLaunchSetup,
                    modifier = Modifier.fillMaxWidth(.85f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.robot_setup),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Spacer(modifier = Modifier.weight(0.12f))
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

private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "robot_thumb_${UUID.randomUUID()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun buildTopicOptions(allTopics: List<RosTopicInfo>, preferredTypes: List<String>): List<TopicBinding> {
    return allTopics.filter { it.type in preferredTypes }.map { TopicBinding(it.name, it.type) }
}

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
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        singleLine = true,
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
fun MainMenuPanel(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .aspectRatio(1.16f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF02101A).copy(alpha = 0.05f),
                                Color(0xFF02101A).copy(alpha = 0.16f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 30.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
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
    options: List<TopicBinding>,
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
            options.forEach { option ->
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
                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        if (request?.isForMainFrame == true) {
                            onLoadingStateChanged(false, error?.description?.toString())
                            // Kill the default Android error icon
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
        update = { it.visibility = android.view.View.VISIBLE; it.loadUrl(url) },
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
        // This is where the WebView sits "behind" the doors
        videoContent()

        if (hatchOpen) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val primaryMsg = if (videoUrl.isNotBlank()) "SIGNAL LINK ERROR" else "NO VIDEO FEED CONFIGURED"
                val secondaryMsg = if (videoUrl.isNotBlank()) "CHECK NETWORK STATUS" else "HARDWARE LINK REQUIRED"

                Icon(
                    imageVector = if (videoUrl.isNotBlank()) Icons.Default.SignalWifiStatusbarConnectedNoInternet4 else Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = HudBlue.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
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

        // The Hatch doors sit on the very top layer
        HatchOverlay(
            modifier = Modifier.matchParentSize(),
            isOpen = hatchOpen
        )

        // Overlay text if it's strictly offline/error before opening
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

        // Left door
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

        // Right door
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
    onDismiss: () -> Unit,
    onSave: (RobotConfig, Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onBackToMenu: () -> Unit
) {
    var haptics by remember { mutableStateOf(initialHaptics) }

    CyberDialog(
        show = true,
        title = "SYSTEM CONFIG",
        confirmText = "APPLY ▶",
        onConfirm = { onSave(currentRobot, haptics) },
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HudBlue.copy(alpha = 0.05f))
                    .border(1.dp, HudBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Checkbox(
                    checked = haptics,
                    onCheckedChange = { haptics = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = HudBlue,
                        uncheckedColor = HudBlue.copy(alpha = 0.4f),
                        checkmarkColor = HudBackground
                    )
                )
                Text(
                    text = "ENABLE HAPTIC FEEDBACK",
                    color = HudText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            CyberButton(
                onClick = onBackToMenu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .background(Color.Red.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TERMINATE SESSION",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
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
            HudTextField(value = cmd, onValueChange = { cmd = it }, label = "ROS COMMAND (e.g. walk)")
        }
    }
}