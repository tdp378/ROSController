package com.example.jaxgamepad

import com.example.jaxgamepad.ui.HudIndicator

data class TopicBinding(
    val name: String,
    val type: String
)

data class RobotMode(
    val label: String,
    val command: String
)

data class RobotConfig(
    val name: String,
    val rosAddress: String,
    val videoUrl: String,
    val thumbnailPath: String? = null,

    val cmdVelTopic: TopicBinding? = null,
    val modeTopic: TopicBinding? = null,
    val batteryTopic: TopicBinding? = null,
    val imuTopic: TopicBinding? = null,
    val odomTopic: TopicBinding? = null,
    val jointStateTopic: TopicBinding? = null,

    val modes: List<RobotMode> = listOf(
        RobotMode("STAND", "stand"),
        RobotMode("WALK", "walk"),
        RobotMode("SIT", "sit"),
        RobotMode("LAY", "lay"),
        RobotMode("SHAKE", "shake"),
        RobotMode("WAVE", "wave")
    ),

    val enabledIndicators: List<String> = HudIndicator.entries.map { it.name },

    val totalUptimeSeconds: Long = 0L,
    val totalDistanceMeters: Double = 0.0
)