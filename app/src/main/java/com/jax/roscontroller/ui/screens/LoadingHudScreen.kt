package com.jax.roscontroller.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jax.roscontroller.ui.theme.JaxGamepadTheme
import com.jax.roscontroller.ui.theme.MyColors
import kotlinx.coroutines.delay

@Composable
fun LoadingHudScreen(
    robotName: String,
    onFinished: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("INITIALIZING...") }

    val statusMessages = listOf(
        "INITIALIZING CORE SYSTEMS...",
        "ESTABLISHING SECURE UPLINK...",
        "SYNCHRONIZING TELEMETRY DATA...",
        "MOUNTING HUD OVERLAY...",
        "SYSTEMS READY - LINK STABLE"
    )

    LaunchedEffect(Unit) {
        val duration = 3000L
        val startTime = System.currentTimeMillis()
        
        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / duration).coerceAtMost(1f)
            
            val msgIndex = (progress * (statusMessages.size - 1)).toInt()
            statusText = statusMessages[msgIndex]
            
            delay(20)
        }
        delay(1000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(32.dp)
        ) {
            Text(
                text = "BOOTING: ${robotName.uppercase()}",
                color = MyColors.HudBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Progress Bar Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .border(1.dp, MyColors.HudBlue.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    .padding(2.dp)
            ) {
                // Background of bar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MyColors.HudBlue.copy(alpha = 0.1f))
                )
                // Filling bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MyColors.HudBlue.copy(alpha = 0.5f),
                                    MyColors.HudBlue
                                )
                            )
                        )
                )
            }

            Text(
                text = statusText,
                color = MyColors.HudBlue.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        
        // Corner accents
        HudCornerAccents()
    }
}

@Composable
fun HudCornerAccents() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top Left
        Box(modifier = Modifier.size(20.dp).align(Alignment.TopStart)) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MyColors.HudBlue))
            Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(MyColors.HudBlue))
        }
        // Top Right
        Box(modifier = Modifier.size(20.dp).align(Alignment.TopEnd)) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MyColors.HudBlue).align(Alignment.TopEnd))
            Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(MyColors.HudBlue).align(Alignment.TopEnd))
        }
        // Bottom Left
        Box(modifier = Modifier.size(20.dp).align(Alignment.BottomStart)) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MyColors.HudBlue).align(Alignment.BottomStart))
            Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(MyColors.HudBlue).align(Alignment.BottomStart))
        }
        // Bottom Right
        Box(modifier = Modifier.size(20.dp).align(Alignment.BottomEnd)) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MyColors.HudBlue).align(Alignment.BottomEnd))
            Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(MyColors.HudBlue).align(Alignment.BottomEnd))
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun LoadingHudScreenPreview() {
    JaxGamepadTheme {
        LoadingHudScreen(
            robotName = "Unit-01",
            onFinished = {}
        )
    }
}
