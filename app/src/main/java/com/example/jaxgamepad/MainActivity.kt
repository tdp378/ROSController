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
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.jaxgamepad.ui.screens.JaxDriverScreen
import com.example.jaxgamepad.ui.screens.RobotSetupScreen
import com.example.jaxgamepad.ui.screens.StartMenuScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

enum class Screen { Splash, Menu, Gamepad, RobotSetup }


@Composable
fun GlobalTopBar(user: com.google.firebase.auth.FirebaseUser?, onOpenAccount: () -> Unit, onOpenHelp: () -> Unit) {
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
                if (user?.photoUrl != null) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
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
}
@Composable
fun AppNavigation(reHideSystemBars: () -> Unit) {
    val context = LocalContext.current
    val robotManager = remember { RobotManager(context) }
    var signedInUser by remember { mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null) }
    var terminalText by remember { mutableStateOf("") }
    var hasBooted by remember { mutableStateOf(false) }
    val networkInfo by produceState(initialValue = "SCANNING" to "0.0.0.0") {


        while (true) {
            value = getNetworkDetails(context)
            delay(5000)
        }
    }

// 1. Centralize the text logic so you don't repeat yourself
    val bootScript by remember(networkInfo, signedInUser) {
        derivedStateOf {
            val (status, addr) = networkInfo
            val isConnected = status == "WIFI_CONNECTED"
            val userEmail = signedInUser?.email ?: "GUEST"
            val userName = signedInUser?.displayName?.takeIf { it.isNotBlank() }
                ?: signedInUser?.email?.substringBefore("@")
                ?: "GUEST"


            // Define the template once
            buildString {
                appendLine("> BOOTING_ROS_CONTROLLER")
                appendLine(">")
                appendLine("> USER: ${userName.uppercase().replace(" ", "_")}")
                appendLine("> LINK: $status")
                if (isConnected) {
                    appendLine("> IP: $addr")
                    appendLine(">")
                    append("> STATUS: SYSTEM_READY")
                } else {
                    appendLine("> IP: UNKNOWN")
                    appendLine("> ERROR: NO_LOCAL_IP_FOUND")
                    append("> STATUS: SYSTEM_OFFLINE")
                }
            }.uppercase()
        }
    }

    var currentScreen by remember { mutableStateOf(Screen.Splash) }

    // 2. Use one effect to bridge the gap
    LaunchedEffect(bootScript, currentScreen) {
        if (currentScreen == Screen.Menu && !hasBooted) {
            // Run the typewriter animation ONLY the first time when we arrive at the menu
            terminalText = ""
            bootScript.forEach { char ->
                terminalText += char
                delay(30)
            }
            hasBooted = true
        } else if (hasBooted) {
            // After the first boot, just snap to the new text instantly
            terminalText = bootScript
        }
    }




    var savedRobots by remember {
        mutableStateOf(loadRobotsForOwner(robotManager, signedInUser?.uid))
    }

    
    LaunchedEffect(Unit) {
        if (currentScreen == Screen.Splash) {
            delay(2500) // Show splash for 2.5 seconds
            currentScreen = Screen.Menu
        }
    }

    var currentRobot by remember {
        mutableStateOf(savedRobots.firstOrNull() ?: buildDemoRobot(RobotManager.normalizeOwnerUid(signedInUser?.uid)))
    }
    var hapticsEnabled by remember { mutableStateOf(true) }

    val ros = remember { RosbridgeClient() }
    val activity = LocalContext.current as? ComponentActivity

    val portraitScreens = setOf(Screen.Splash, Screen.Menu, Screen.RobotSetup)

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
                    // Stay on the dialog to show the success state/profile details
                }.onFailure { e ->
                    Log.e("GOOGLE_AUTH", "Sign in failed", e)
                    val message = when (e) {
                        is androidx.credentials.exceptions.GetCredentialCancellationException -> "LOGIN_CANCELLED_BY_USER"
                        is androidx.credentials.exceptions.NoCredentialException -> "NO_GOOGLE_ACCOUNT_FOUND_ON_DEVICE"
                        else -> "AUTH_SYSTEM_ERROR: ${e.localizedMessage ?: "UNKNOWN"}"
                    }
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        },
        onSignOut = {
            FirebaseAuth.getInstance().signOut()
            showAccountDialog = false
        },
        onDeleteAccount = {
            val user = FirebaseAuth.getInstance().currentUser
            user?.let { u ->
                val db = FirebaseFirestore.getInstance()
                // Delete user data from Firestore first
                db.collection("users").document(u.uid).delete()
                    .addOnCompleteListener { firestoreTask ->
                        // Even if firestore delete fails, we should try to delete auth user
                        // But usually we want both.
                        u.delete().addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                android.widget.Toast.makeText(context, "ACCOUNT PURGED FROM SYSTEM", android.widget.Toast.LENGTH_LONG).show()
                                showAccountDialog = false
                            } else {
                                val error = authTask.exception?.message ?: "Unknown error"
                                if (error.contains("recent login", ignoreCase = true)) {
                                    android.widget.Toast.makeText(context, "SECURITY: Please re-login to verify identity before purging", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "PURGE FAILED: $error", android.widget.Toast.LENGTH_LONG).show()
                                }
                                Log.e("AUTH", "Failed to delete account: $error", authTask.exception)
                            }
                        }
                    }
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        if (currentScreen != Screen.Gamepad) {
            GlobalTopBar(
                user = signedInUser,
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
                Screen.Splash -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.splash),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.6f),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Screen.Menu -> StartMenuScreen(
                    ros = ros,
                    savedRobots = savedRobots,
                    terminalText = terminalText,
                    isBootComplete = hasBooted,
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
                            // Sync to cloud if signed in
                            if (signedInUser != null) {
                                saveRobotConfigToFirestore(normalizedRobot)
                            }
                        } else {
                            val nameIdx = list.indexOfFirst { it.name == normalizedRobot.name }
                            if (nameIdx != -1) {
                                list[nameIdx] = normalizedRobot
                            } else {
                                list.add(normalizedRobot)
                            }
                            savedRobots = list
                            robotManager.saveRobots(list, ownerUid)
                            if (signedInUser != null) {
                                saveRobotConfigToFirestore(normalizedRobot)
                            }
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
