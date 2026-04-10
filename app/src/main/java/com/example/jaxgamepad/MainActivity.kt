package com.example.jaxgamepad

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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




    var savedRobots by remember {
        mutableStateOf(loadRobotsForOwner(robotManager, signedInUser?.uid))
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


    RobotSyncLogic(
        signedInUser = signedInUser,
        robotManager = robotManager,
        currentRobot = currentRobot,
        onUpdateRobots = { savedRobots = it },
        onUpdateCurrentRobot = { currentRobot = it }
    )

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
                        } else {
                            // If not found by ID (shouldn't happen with stable IDs), try name or add it
                            val nameIdx = list.indexOfFirst { it.name == normalizedRobot.name }
                            if (nameIdx != -1) {
                                list[nameIdx] = normalizedRobot
                            } else {
                                list.add(normalizedRobot)
                            }
                            savedRobots = list
                            robotManager.saveRobots(list, ownerUid)
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
