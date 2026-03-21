package com.example.jaxgamepad

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToInt

// --- THEME COLORS ---
val NeonCyan = Color(0xFF00E5FF)
val DeepNavy = Color(0xFF050B10)
val CyberDark = Color(0xFF0A141A)

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

enum class Screen { Menu, Gamepad, RobotStatus }

@Composable
fun AppNavigation(reHideSystemBars: () -> Unit) {
    var currentScreen by remember { mutableStateOf(Screen.Menu) }
    var rosAddress by remember { mutableStateOf("192.168.1.50:9090") }
    var videoUrl by remember { mutableStateOf("http://192.168.1.50:8080/stream?topic=/camera/image_raw") }
    var hapticsEnabled by remember { mutableStateOf(true) }

    val ros = remember { RosbridgeClient() }
    val activity = LocalContext.current as MainActivity

    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.RobotStatus || currentScreen == Screen.Menu) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            reHideSystemBars()
        }
    }

    when (currentScreen) {
        Screen.Menu -> StartMenuScreen(
            ros = ros,
            onLaunchGamepad = { currentScreen = Screen.Gamepad },
            onLaunchStatus = { currentScreen = Screen.RobotStatus }
        )
        Screen.Gamepad -> JaxDriverScreen(
            ros = ros,
            rosAddress = rosAddress,
            videoUrl = videoUrl,
            hapticsEnabled = hapticsEnabled,
            onSettingsChange = { addr, url, haptics ->
                rosAddress = addr; videoUrl = url; hapticsEnabled = haptics
            },
            reHideSystemBars = reHideSystemBars,
            onBackToMenu = { currentScreen = Screen.Menu }
        )
        Screen.RobotStatus -> RobotStatusScreen(
            rosAddress = rosAddress,
            isConnected = ros.isConnected,
            onBack = { currentScreen = Screen.Menu }
        )
    }
}

// --- UI HELPER COMPONENTS ---

@Composable
fun HudDataLabel(label: String, value: String) {
    Column {
        Text(text = label, color = NeonCyan.copy(alpha = 0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CyberJoystickContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(150.dp)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFF1A2126), DeepNavy)),
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.2f),
                    style = Stroke(width = 3f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun GlowModeButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(80.dp)
            .height(40.dp)
            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy.copy(alpha = 0.7f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// --- MAIN SCREENS ---

@Composable
fun JaxDriverScreen(
    ros: RosbridgeClient,
    rosAddress: String,
    videoUrl: String,
    hapticsEnabled: Boolean,
    onSettingsChange: (String, String, Boolean) -> Unit,
    reHideSystemBars: () -> Unit,
    onBackToMenu: () -> Unit
) {
    var moveX by remember { mutableStateOf(0.0) }
    var moveY by remember { mutableStateOf(0.0) }
    var turnZ by remember { mutableStateOf(0.0) }
    var showSettings by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var publishJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(ros.isConnected) {
        if (ros.isConnected && publishJob?.isActive != true) {
            publishJob = scope.launch {
                while (true) {
                    ros.publishCmdVel(moveY, moveX, turnZ)
                    delay(75)
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            initialRosAddress = rosAddress,
            initialVideoUrl = videoUrl,
            initialHaptics = hapticsEnabled,
            onDismiss = { showSettings = false; reHideSystemBars() },
            onSave = { addr, url, haptics ->
                onSettingsChange(addr, url, haptics)
                ros.connect("ws://$addr")
                showSettings = false
                reHideSystemBars()
            },
            onDisconnect = { ros.disconnect() },
            extraContent = {
                TextButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) {
                    Text("BACK TO MAIN MENU", color = NeonCyan.copy(alpha = 0.8f))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(CyberDark, Color.Black)))
    ) {
        // --- TOP HUD BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(if (ros.isConnected) Color.Green else Color.Red, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (ros.isConnected) "BRIDGE: LIVE" else "BRIDGE: OFFLINE",
                    color = if (ros.isConnected) Color.Green.copy(0.7f) else Color.Red.copy(0.7f),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
            Text("JAX 1.0 CONTROLLER", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = NeonCyan)
            }
        }

        // --- CENTRAL VIDEO FEED ---
        Box(modifier = Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(width = 400.dp, height = 220.dp)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                color = Color.Black,
                shape = RoundedCornerShape(12.dp)
            ) {
                MjpegWebView(url = videoUrl, onLoadingStateChanged = { _, _ -> }, onUserInteraction = reHideSystemBars)
            }
            Column(modifier = Modifier.offset(x = (-260).dp, y = (-20).dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HudDataLabel("BATTERY", "78%")
                HudDataLabel("LATENCY", "24ms")
            }
            Column(modifier = Modifier.offset(x = 260.dp, y = (-20).dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HudDataLabel("STATE", "STAND")
                HudDataLabel("SPEED", "0.5m/s")
            }
        }

        // --- CONTROLS ---
        CyberJoystickContainer(modifier = Modifier.align(Alignment.BottomStart).padding(start = 30.dp, bottom = 30.dp)) {
            TransparentJoystick(hapticsEnabled, onChange = { x, y -> moveX = x * 0.5; moveY = y * 0.8 })
        }

        CyberJoystickContainer(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 30.dp)) {
            TransparentTurnPad(hapticsEnabled, onChange = { z -> turnZ = z * 1.0 })
        }

        // --- BOTTOM ACTION ROW ---
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowModeButton("SIT") { ros.publishMode("sit") }
                GlowModeButton("STAND") { ros.publishMode("stand") }
            }
            Button(
                onClick = { moveX = 0.0; moveY = 0.0; turnZ = 0.0; ros.publishCmdVel(0.0, 0.0, 0.0) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCCB00020)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(88.dp).width(100.dp).border(2.dp, Color.Red, RoundedCornerShape(8.dp))
            ) {
                Text("STOP", fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowModeButton("WALK") { ros.publishMode("walk") }
                GlowModeButton("LAY") { ros.publishMode("lay") }
            }
        }
    }
}

@Composable
fun RobotStatusScreen(rosAddress: String, isConnected: Boolean, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(CyberDark).statusBarsPadding().padding(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("TELEMETRY HUB", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Divider(color = NeonCyan.copy(alpha = 0.2f))
            StatusCard("CONNECTION", if (isConnected) "ACTIVE" else "OFFLINE", if (isConnected) Color.Green else Color.Red)
            StatusCard("ROS IP", rosAddress, NeonCyan)
            StatusCard("BATTERY", "78% (12.4V)", NeonCyan)
            StatusCard("PI TEMP", "48°C", Color.Yellow)
            StatusCard("UPTIME", "00:14:22", Color.White)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = NeonCyan),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) { Text("RETURN TO CONTROLS") }
        }
    }
}

@Composable
fun StatusCard(label: String, value: String, valueColor: Color) {
    Surface(color = Color.White.copy(0.05f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun StartMenuScreen(ros: RosbridgeClient, onLaunchGamepad: () -> Unit, onLaunchStatus: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.jax_background), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)))
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
            MenuButton("Launch Controller", onClick = onLaunchGamepad)
            Spacer(modifier = Modifier.height(16.dp))
            MenuButton("Robot Status", onClick = onLaunchStatus)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.85f).height(56.dp).border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy.copy(alpha = 0.9f), contentColor = NeonCyan)
    ) { Text(text.uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
}

class RosbridgeClient {
    private val client = OkHttpClient()
    private var socket: WebSocket? = null
    var isConnected by mutableStateOf(false)
    var statusText by mutableStateOf("Disconnected")

    fun connect(url: String) {
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { isConnected = true; statusText = "Connected"; advertiseTopics() }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { isConnected = false }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { isConnected = false }
        })
    }
    fun disconnect() { socket?.close(1000, "bye"); isConnected = false }
    private fun advertiseTopics() {
        socket?.send(JSONObject().apply { put("op", "advertise"); put("topic", "/cmd_vel"); put("type", "geometry_msgs/msg/Twist") }.toString())
        socket?.send(JSONObject().apply { put("op", "advertise"); put("topic", "/jax_mode"); put("type", "std_msgs/msg/String") }.toString())
    }
    fun publishCmdVel(linearX: Double, linearY: Double, angularZ: Double) {
        val msg = JSONObject().apply {
            put("op", "publish"); put("topic", "/cmd_vel")
            put("msg", JSONObject().apply {
                put("linear", JSONObject().apply { put("x", linearX); put("y", linearY); put("z", 0.0) })
                put("angular", JSONObject().apply { put("x", 0.0); put("y", 0.0); put("z", angularZ) })
            })
        }
        socket?.send(msg.toString())
    }
    fun publishMode(mode: String) {
        val msg = JSONObject().apply {
            put("op", "publish"); put("topic", "/jax_mode")
            put("msg", JSONObject().apply { put("data", mode) })
        }
        socket?.send(msg.toString())
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MjpegWebView(url: String, onLoadingStateChanged: (loaded: Boolean, error: String?) -> Unit, onUserInteraction: () -> Unit, modifier: Modifier = Modifier) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            loadUrl(url)
        }
    }, modifier = modifier)
}

@Composable
fun TransparentJoystick(hapticsEnabled: Boolean, modifier: Modifier = Modifier, onChange: (x: Double, y: Double) -> Unit) {
    val size = 150.dp
    val knob = 60.dp
    val sizePx = with(LocalDensity.current) { size.toPx() }
    val knobPx = with(LocalDensity.current) { knob.toPx() }
    val radius = (sizePx - knobPx) / 2f
    var offset by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier.size(size).pointerInput(hapticsEnabled) {
            detectDragGestures(
                onDragEnd = { offset = Offset.Zero; onChange(0.0, 0.0) },
                onDragCancel = { offset = Offset.Zero; onChange(0.0, 0.0) }
            ) { change, dragAmount ->
                change.consume()
                val newOffset = offset + dragAmount
                val dist = newOffset.getDistance()
                if (dist >= radius && hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                offset = if (dist >= radius) newOffset * (radius / dist) else newOffset
                onChange((offset.x / radius).toDouble(), (-offset.y / radius).toDouble())
            }
        }, contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }.size(knob)
                .background(Brush.radialGradient(listOf(Color(0xFF444D55), Color(0xFF101418))), CircleShape)
                .border(2.dp, NeonCyan, CircleShape)
        )
    }
}

@Composable
fun TransparentTurnPad(hapticsEnabled: Boolean, modifier: Modifier = Modifier, onChange: (z: Double) -> Unit) {
    val size = 150.dp
    val knob = 60.dp
    val sizePx = with(LocalDensity.current) { size.toPx() }
    val knobPx = with(LocalDensity.current) { knob.toPx() }
    val radius = (sizePx - knobPx) / 2f
    var offsetX by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier.size(size).pointerInput(hapticsEnabled) {
            detectDragGestures(
                onDragEnd = { offsetX = 0f; onChange(0.0) },
                onDragCancel = { offsetX = 0f; onChange(0.0) }
            ) { change, dragAmount ->
                change.consume()
                val newX = offsetX + dragAmount.x
                if (abs(newX) >= radius && hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                offsetX = newX.coerceIn(-radius, radius)
                onChange((offsetX / radius).toDouble())
            }
        }, contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }.size(knob)
                .background(Brush.radialGradient(listOf(Color(0xFF444D55), Color(0xFF101418))), CircleShape)
                .border(2.dp, NeonCyan, CircleShape)
        )
    }
}

@Composable
fun SettingsDialog(initialRosAddress: String, initialVideoUrl: String, initialHaptics: Boolean, onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit, onDisconnect: () -> Unit, extraContent: @Composable () -> Unit = {}) {
    var rosAddress by remember { mutableStateOf(initialRosAddress) }
    var videoUrl by remember { mutableStateOf(initialVideoUrl) }
    var haptics by remember { mutableStateOf(initialHaptics) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = CyberDark, border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("SYSTEM SETTINGS", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(value = rosAddress, onValueChange = { rosAddress = it }, label = { Text("ROS Bridge Address", color = NeonCyan.copy(alpha = 0.7f)) }, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("Video Stream URL", color = NeonCyan.copy(alpha = 0.7f)) }, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = haptics, onCheckedChange = { haptics = it }, colors = CheckboxDefaults.colors(checkedColor = NeonCyan)); Text("Enable Haptics", color = Color.White) }
                Button(onClick = { onSave(rosAddress, videoUrl, haptics) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepNavy)) { Text("SAVE & CONNECT") }
                Button(onClick = onDisconnect, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)) { Text("DISCONNECT") }
                extraContent()
            }
        }
    }
}