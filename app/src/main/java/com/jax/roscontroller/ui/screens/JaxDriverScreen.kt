package com.jax.roscontroller.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jax.roscontroller.RobotConfig
import com.jax.roscontroller.RosbridgeClient
import com.jax.roscontroller.ui.theme.JaxGamepadTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jax.roscontroller.CyberButton
import com.jax.roscontroller.CyberDialog
import com.jax.roscontroller.IndicatorRockerRow
import com.jax.roscontroller.R
import com.jax.roscontroller.SettingsRockerRow
import com.jax.roscontroller.formatUptime
import com.jax.roscontroller.ui.theme.MyColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

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

    val currentOnRobotChange by rememberUpdatedState(onRobotChange)
    val latestRobot by rememberUpdatedState(currentRobot)

    ros.isConnected
    val sessionBaseUptime = remember(currentRobot.name) { currentRobot.totalUptimeSeconds }
    val baseDistance = remember(currentRobot.name) { currentRobot.totalDistanceMeters }
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

    var videoRefreshNonce by remember { mutableIntStateOf(0) }
    var videoError by remember(currentRobot.videoUrl) { mutableStateOf<String?>(null) }
    var videoButtonActive by remember(currentRobot.videoUrl) { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var cameraServerOnline by remember { mutableStateOf(false) }

    LaunchedEffect(ros.isConnected) {
        if (ros.isConnected) {
            // Force video reload when ROS reconnects
            videoRefreshNonce++
        } else {
            // If ROS is down, we assume the camera server might be unreachable too
            cameraServerOnline = false
        }
    }

    LaunchedEffect(currentRobot.rosAddress, reconnectNonce) {
        if (currentRobot.rosAddress.isNotBlank()) {
            ros.disconnect()
            ros.connect("ws://${currentRobot.rosAddress}") {
                ros.advertiseIfNeeded(latestRobot)
                ros.subscribeToTelemetry(latestRobot)
            }
        }
    }

    DisposableEffect(currentRobot.name) {
        onDispose { ros.clearTelemetrySubscriptions(latestRobot) }
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
                currentOnRobotChange(
                    latestRobot.copy(
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
                currentOnRobotChange(
                    latestRobot.copy(
                        totalUptimeSeconds = sessionBaseUptime + sessionSeconds,
                        totalDistanceMeters = baseDistance + ros.totalDistance
                    )
                )
                lastSavedSessionSeconds = sessionSeconds
            }
        }
    }

    DisposableEffect(currentRobot.name) {
        onDispose {
            if (sessionSeconds > lastSavedSessionSeconds || ros.totalDistance > 0) {
                currentOnRobotChange(
                    latestRobot.copy(
                        totalUptimeSeconds = sessionBaseUptime + sessionSeconds,
                        totalDistanceMeters = baseDistance + ros.totalDistance
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
                    videoRefreshNonce++ // Also refresh video on manual reconnect
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
                color = MyColors.HudText,
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
        batteryVoltage = ros.lastBatteryVoltage,
        cpuTemp = ros.lastCpuTemp ?: 0,
        isLinked = ros.isConnected,
        odomActive = ros.isOdomActive && currentRobot.odomTopic != null,
        totalDistance = baseDistance + ros.totalDistance,
        imuActive = ros.isImuActive && currentRobot.imuTopic != null,
        cameraActive = cameraServerOnline,
        batteryActive = ros.isConnected && currentRobot.batteryTopic != null,
        cpuActive = ros.isConnected && currentRobot.cpuTempTopic != null,
        selectedMode = ros.lastModeText ?: currentRobot.modes.firstOrNull()?.command ?: "walk",
        modes = currentRobot.modes,
        enabledIndicators = activeIndicators,
        leftJoystickValue = moveX.toFloat() to moveY.toFloat(),
        rightJoystickValue = turnZ.toFloat() to 0f,
        heightSliderValue = bodyHeightZ.toFloat(),
        videoActive = videoButtonActive,
        hapticsEnabled = hapticsEnabled,
        onVideoToggle = { enabled ->
            videoButtonActive = enabled
            if (enabled) {
                videoError = null
            }
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
                hatchOpen = videoButtonActive,
                errorText = videoError,
                videoUrl = currentRobot.videoUrl
            ) {
                if (currentRobot.videoUrl.isNotBlank()) {
                    MjpegWebView(
                        url = currentRobot.videoUrl,
                        refreshNonce = videoRefreshNonce,
                        modifier = Modifier.fillMaxSize(),
                        onLoadingStateChanged = { loaded, error ->
                            if (loaded) {
                                videoError = null
                                cameraServerOnline = true 
                            } else if (error != null) {
                                videoError = error
                            }
                        },
                        onUserInteraction = reHideSystemBars
                    )
                }
            }
        }
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
            .border(1.dp, MyColors.HudBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        videoContent()

        if (hatchOpen && (errorText != null || videoUrl.isBlank())) {
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
                    tint = MyColors.HudBlue.copy(alpha = 0.5f),
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = primaryMsg,
                    color = MyColors.HudBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = secondaryMsg,
                    color = MyColors.HudBlue.copy(alpha = 0.6f),
                    fontSize = 8.sp
                )
                if (videoUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = videoUrl,
                        color = MyColors.HudText.copy(alpha = 0.4f),
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
                color = MyColors.HudBlue,
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
                    val leds = listOf(HudIndicator.ROS_LINK, HudIndicator.ODOM, HudIndicator.IMU, HudIndicator.CAMERA)
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MjpegWebView(
    url: String,
    onLoadingStateChanged: (loaded: Boolean, error: String?) -> Unit,
    onUserInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    refreshNonce: Int = 0
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setBackgroundColor(android.graphics.Color.BLACK)
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingStateChanged(true, null)
                        // Absolute positioning is more reliable for centering + filling
                        view?.evaluateJavascript("""
                            (function() {
                                var style = document.createElement('style');
                                style.innerHTML = 'body { margin: 0; background: black; height: 100vh; width: 100vw; overflow: hidden; position: relative; } img { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 100%; height: 100%; object-fit: cover; }';
                                document.head.appendChild(style);
                            })()
                        """.trimIndent(), null)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onLoadingStateChanged(false, error?.description?.toString() ?: "Error")
                        }
                    }
                }
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                
                setOnTouchListener { _, _ ->
                    onUserInteraction()
                    false
                }
                
                val finalUrl = if (!url.startsWith("http") && !url.startsWith("https")) "http://$url" else url
                loadUrl(finalUrl)
                tag = "$url-$refreshNonce"
            }
        },
        update = {
            val currentTag = "$url-$refreshNonce"
            if (it.tag != currentTag) {
                it.tag = currentTag
                val finalUrl = if (!url.startsWith("http") && !url.startsWith("https")) "http://$url" else url
                it.loadUrl(finalUrl)
            }
        },
        modifier = modifier
    )
}

private fun generateMjpegHtml(url: String): String {
    if (url.isBlank()) return "<html><body style='background:black;'></body></html>"
    val absoluteUrl = if (!url.startsWith("http")) "http://$url" else url

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                body { 
                    margin: 0; 
                    padding: 0; 
                    background-color: black; 
                    display: flex; 
                    justify-content: center; 
                    align-items: center; 
                    height: 100vh; 
                    width: 100vw; 
                    overflow: hidden; 
                }
                img { 
                    width: 100%; 
                    height: 100%; 
                    object-fit: cover; 
                }
            </style>
        </head>
        <body>
            <img src="$absoluteUrl" />
        </body>
        </html>
    """.trimIndent()
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun JaxDriverScreenPreview() {
    JaxGamepadTheme {
        JaxDriverScreen(
            ros = RosbridgeClient(),
            currentRobot = RobotConfig(
                name = "JAX-01",
                rosAddress = "192.168.1.100:9090",
                videoUrl = "http://192.168.1.100:8080"
            ),
            savedRobots = emptyList(),
            hapticsEnabled = true,
            onRobotChange = {},
            onHapticsChange = {},
            reHideSystemBars = {},
            onBackToMenu = {},
            networkInfo = "WiFi" to "Connected"
        )
    }
}

