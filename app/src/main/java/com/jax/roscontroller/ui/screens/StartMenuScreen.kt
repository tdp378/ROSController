package com.jax.roscontroller.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jax.roscontroller.CyberButton
import com.jax.roscontroller.CyberDialog

import com.jax.roscontroller.R
import com.jax.roscontroller.RobotConfig
import com.jax.roscontroller.RobotSelectionDialog
import com.jax.roscontroller.RosbridgeClient
import com.jax.roscontroller.ui.theme.MyColors


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




@Composable
fun StartMenuScreen(
    ros: RosbridgeClient,
    savedRobots: List<RobotConfig>,
    terminalText: String,
    isBootComplete: Boolean,
    isSignedIn: Boolean,
    signedInLabel: String,
    onLaunchGamepad: (RobotConfig) -> Unit,
    onLaunchSetup: () -> Unit,
    onOpenAccount: () -> Unit
) {
    var showRobotSelectDialog by remember { mutableStateOf(false) }
    var showNoRobotWarning by remember { mutableStateOf(false) }

    val contentAlpha by animateFloatAsState(
        targetValue = if (isBootComplete) 1f else 0f,
        animationSpec = tween(1200),
        label = "menu_fade"
    )

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
            color = MyColors.HudText,
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(id = R.drawable.bg_main),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(contentAlpha),
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
                        color = MyColors.TerminalCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Box(modifier = Modifier.alpha(contentAlpha)) {
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
}

