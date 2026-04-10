package com.example.jaxgamepad.ui.screens

import android.net.Uri
import androidx.compose.ui.tooling.preview.Preview
import com.example.jaxgamepad.ui.theme.JaxGamepadTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jaxgamepad.CyberButton
import com.example.jaxgamepad.CyberDialog
import com.example.jaxgamepad.HelpDialog
import com.example.jaxgamepad.HudTextField
import com.example.jaxgamepad.R
import com.example.jaxgamepad.RobotConfig
import com.example.jaxgamepad.RobotManager
import com.example.jaxgamepad.RobotMode
import com.example.jaxgamepad.RosTopicInfo
import com.example.jaxgamepad.RosbridgeClient
import com.example.jaxgamepad.TopicBinding
import com.example.jaxgamepad.buildTopicOptions
import com.example.jaxgamepad.formatDistance
import com.example.jaxgamepad.formatUptime
import com.example.jaxgamepad.saveImageToInternalStorage
import com.example.jaxgamepad.saveRobotConfigToFirestore
import com.example.jaxgamepad.ui.theme.MyColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.forEach
import kotlin.toString

data class TopicDropdownItem(
    val binding: TopicBinding? = null,
    val label: String? = null,
    val isHeader: Boolean = false
)


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
                color = MyColors.HudBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text("• Run robridge - ws://192.168.x.x:9090", color = MyColors.HudText, fontSize = 13.sp)
            Text("• Motion topic using - geometry_msgs/Twist", color = MyColors.HudText, fontSize = 13.sp)
            Text("• Mode topic using - std_msgs/String", color = MyColors.HudText, fontSize = 13.sp)
            Text("Optional:", color = MyColors.HudBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("• MJPEG stream for video support", color = MyColors.HudText, fontSize = 13.sp)
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
                    color = MyColors.HudText,
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
                    color = MyColors.HudBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = firestoreStatus,
                    color = if (firestoreStatus.startsWith("CLOUD SYNC FAILED")) Color.Red else MyColors.HudBlue,
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
            .background(MyColors.HudBackground)

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
                            color = MyColors.HudBlue,
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
                                .background(MyColors.HudBlue.copy(alpha = 0.1f), CircleShape)
                                .border(1.dp, MyColors.HudBlue.copy(alpha = 0.4f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add New",
                                tint = MyColors.HudBlue
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
                                color = MyColors.HudBlue.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MyColors.HudBlue.copy(alpha = 0.2f)),
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
                                        tint = MyColors.HudBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "SYSTEM NOTIFICATION",
                                            color = MyColors.HudBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ROSbot is provided as a sample robot for you to test the UI. Configure your own robot to enable real-time telemetry and motion control.",
                                            color = MyColors.HudText.copy(alpha = 0.8f),
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
                            color = MyColors.HudText,
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
                        contentColor = MyColors.HudBlue,
                        indicator = { tabPositions ->
                            if (selectedTabOrStep < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabOrStep]),
                                    color = MyColors.HudBlue
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
                                        color = if (isEnabled) MyColors.HudText else MyColors.HudBlue
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
                                            color = MyColors.HudText.copy(alpha = 0.3f),
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
                                            tint = MyColors.HudText.copy(alpha = 0.4f)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "UPTIME : ${formatUptime(initial.totalUptimeSeconds)}",
                                        color = MyColors.HudText.copy(alpha = .7f),
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp
                                    )

                                    Text(
                                        text = "TRAVELED: ${formatDistance(initial.totalDistanceMeters)}",
                                        color = MyColors.HudText.copy(alpha = .7f),
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            MyColors.HudTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = "ROBOT NAME *"
                            )
                            MyColors.HudTextField(
                                value = addr,
                                onValueChange = { addr = it },
                                label = "ROS BRIDGE IP (IP:PORT) *"
                            )
                            MyColors.HudTextField(
                                value = url,
                                onValueChange = { url = it },
                                label = "VIDEO SERVER URL (OPTIONAL)"
                            )


                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "AXIS SETTINGS",
                                color = MyColors.HudBlue,
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
                                options = buildTopicOptions(
                                    allDiscoveredTopics,
                                    listOf("geometry_msgs/msg/Twist"),
                                    standardOption = TopicBinding("/cmd_vel", "geometry_msgs/msg/Twist")
                                ),
                                placeholder = "Select Topic...",
                                onSelected = { cmdVelTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "MODE TOPIC",
                                selected = modeTopic,
                                options = buildTopicOptions(
                                    allDiscoveredTopics,
                                    listOf("std_msgs/msg/String"),
                                    standardOption = TopicBinding("/robot_mode", "std_msgs/msg/String")
                                ),
                                placeholder = "Select Topic...",
                                onSelected = { modeTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "BATTERY",
                                selected = batteryTopic,
                                options = buildTopicOptions(
                                    allDiscoveredTopics,
                                    listOf("sensor_msgs/msg/BatteryState"),
                                    standardOption = TopicBinding("/battery_state", "sensor_msgs/msg/BatteryState")
                                ),
                                placeholder = "Select Topic...",
                                onSelected = { batteryTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "IMU",
                                selected = imuTopic,
                                options = buildTopicOptions(
                                    allDiscoveredTopics,
                                    listOf("sensor_msgs/msg/Imu"),
                                    standardOption = TopicBinding("/imu/data", "sensor_msgs/msg/Imu")
                                ),
                                placeholder = "Select Topic...",
                                onSelected = { imuTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "ODOM",
                                selected = odomTopic,
                                options = buildTopicOptions(
                                    allDiscoveredTopics,
                                    listOf("nav_msgs/msg/Odometry"),
                                    standardOption = TopicBinding("/odom", "nav_msgs/msg/Odometry")
                                ),
                                placeholder = "Select Topic...",
                                onSelected = { odomTopic = it }
                            )
                            TopicBindingDropdown(
                                title = "JOINT STATES",
                                selected = jointStateTopic,
                                options = buildTopicOptions(
                                    allDiscoveredTopics,
                                    listOf("sensor_msgs/msg/JointState"),
                                    standardOption = TopicBinding("/joint_states", "sensor_msgs/msg/JointState")
                                ),
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
                                    color = MyColors.HudBlue,
                                    letterSpacing = 1.sp
                                )

                                IconButton(
                                    onClick = {
                                        modeToEditIndex = null
                                        showModeDialog = true
                                    },
                                    modifier = Modifier
                                        .background(MyColors.HudBlue.copy(alpha = 0.1f), CircleShape)
                                        .border(1.dp, MyColors.HudBlue.copy(alpha = 0.4f), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = MyColors.HudBlue)
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
fun RobotListItem(
    robot: RobotConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MyColors.HudText.copy(alpha = 0.0f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MyColors.HudText.copy(alpha = 0.3f)),
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
                    .background(MyColors.HudBackground)
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
                        tint = MyColors.HudText.copy(alpha = 0.2f)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = robot.name,
                    color = MyColors.HudText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "UPTIME: ${formatUptime(robot.totalUptimeSeconds)}",
                    color = MyColors.HudText.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.5.sp
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MyColors.HudText.copy(alpha = 0.7f)
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

@Composable
fun ModeEditorRow(
    mode: RobotMode,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MyColors.HudText.copy(alpha = 0.0f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MyColors.HudText.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.label,
                    color = MyColors.HudText,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = MyColors.HudText)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color.Red)
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
            .background(MyColors.HudBlue.copy(alpha = 0.05f))
            .border(1.dp, MyColors.HudBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(enabled = !discovering, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = if (discovering) MyColors.HudBlue else MyColors.HudText.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (discovering) angle else 0f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AUTO DISCOVER TOPICS",
                    color = MyColors.HudBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (!status.isNullOrBlank()) {
                    Text(
                        text = status.uppercase(),
                        color = MyColors.HudText.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
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
                    color = MyColors.HudText.copy(alpha = 0.3f)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(60.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MyColors.HudText.copy(alpha = 0.4f),
                focusedBorderColor = MyColors.HudBlue,
                focusedTextColor = MyColors.HudText,
                unfocusedTextColor = MyColors.HudText,
                unfocusedLabelColor = MyColors.HudBlue,
                focusedLabelColor = MyColors.HudBlue
            ),
            shape = RoundedCornerShape(8.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MyColors.HudSurface)
                .border(1.dp, MyColors.HudBorder, RoundedCornerShape(4.dp))
        ) {
            options.forEach { item ->
                if (item.isHeader) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MyColors.HudBlue.copy(alpha = 0.25f)
                    )
                    Text(
                        text = item.label ?: "",
                        color = MyColors.HudBlue,
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
                                    color = MyColors.HudText,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = option.type,
                                    color = MyColors.HudBlue.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(textColor = MyColors.HudText)
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
                color = MyColors.HudBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            MyColors.HudTextField(value = label, onValueChange = { label = it }, label = "LABEL (e.g. WALK)")
            MyColors.HudTextField(value = cmd, onValueChange = { cmd = it }, label = "COMMAND (e.g. walk)")
        }
    }
}


@Composable
fun AxisSwitchRowDual(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    Surface(
        color = MyColors.HudText.copy(alpha = 0.03f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MyColors.HudText.copy(alpha = 0.4f)),
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
                    .background(MyColors.HudText.copy(alpha = 0.1f))
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
                color = MyColors.HudText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 11.sp
            )

            Text(
                text = if (checked) "INVERTED" else "NORMAL",
                color = if (checked) MyColors.HudBlue else MyColors.HudText.copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 9.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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
                color = MyColors.HudText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 12.sp // 👈 tighter text
            )

            Text(
                text = if (checked) "INVERTED" else "NORMAL",
                color = if (checked) MyColors.HudBlue else MyColors.HudText.copy(alpha = 0.6f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 9.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun RobotSetupScreenPreview() {
    JaxGamepadTheme {
        RobotSetupScreen(
            ros = RosbridgeClient(),
            savedRobots = listOf(
                RobotConfig(
                    name = "ROSbot Demo",
                    rosAddress = "192.168.1.100:9090",
                    videoUrl = "http://192.168.1.100:8080",
                    thumbnailPath = "demo_thumb"
                )
            ),
            onSave = { _, _ -> },
            onDelete = { },
            onBack = { },
            onOpenAccount = { }
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun RobotSetupScreenRobotTabPreview() {
    val sampleRobot = RobotConfig(
        name = "ROSbot Demo",
        rosAddress = "192.168.1.100:9090",
        videoUrl = "http://192.168.1.100:8080",
        thumbnailPath = "demo_thumb"
    )
    JaxGamepadTheme {
        RobotSetupScreen(
            ros = RosbridgeClient(),
            savedRobots = listOf(sampleRobot),
            onSave = { _, _ -> },
            onDelete = { },
            onBack = { },
            onOpenAccount = { },
            initialEditingRobot = sampleRobot,
            initialSelectedTabOrStep = 0
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun RobotSetupScreenTopicsTabPreview() {
    val sampleRobot = RobotConfig(
        name = "ROSbot Demo",
        rosAddress = "192.168.1.100:9090",
        videoUrl = "http://192.168.1.100:8080",
        thumbnailPath = "demo_thumb"
    )
    JaxGamepadTheme {
        RobotSetupScreen(
            ros = RosbridgeClient(),
            savedRobots = listOf(sampleRobot),
            onSave = { _, _ -> },
            onDelete = { },
            onBack = { },
            onOpenAccount = { },
            initialEditingRobot = sampleRobot,
            initialSelectedTabOrStep = 1
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun RobotSetupScreenModesTabPreview() {
    val sampleRobot = RobotConfig(
        name = "ROSbot Demo",
        rosAddress = "192.168.1.100:9090",
        videoUrl = "http://192.168.1.100:8080",
        thumbnailPath = "demo_thumb"
    )
    JaxGamepadTheme {
        RobotSetupScreen(
            ros = RosbridgeClient(),
            savedRobots = listOf(sampleRobot),
            onSave = { _, _ -> },
            onDelete = { },
            onBack = { },
            onOpenAccount = { },
            initialEditingRobot = sampleRobot,
            initialSelectedTabOrStep = 2
        )
    }
}
