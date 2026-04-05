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

    val cmdVelTopic: TopicBinding? = TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
    val modeTopic: TopicBinding? = TopicBinding("/jax_mode", "std_msgs/String"),
    val batteryTopic: TopicBinding? = TopicBinding("/battery", "sensor_msgs/BatteryState"),
    val imuTopic: TopicBinding? = TopicBinding("/imu/data", "sensor_msgs/Imu"),
    val odomTopic: TopicBinding? = TopicBinding("/odom", "nav_msgs/Odometry"),
    val jointStateTopic: TopicBinding? = TopicBinding("/joint_states", "sensor_msgs/JointState"),

    val modes: List<RobotMode> = listOf(
        RobotMode("STAND", "stand"),
        RobotMode("WALK", "walk"),
        RobotMode("SIT", "sit"),
        RobotMode("LAY", "lay"),
        RobotMode("SHAKE", "shake"),
        RobotMode("WAVE", "wave")
    ),

    // NEW: Session Persistence for LED/Telemetry Selection
    // This stores the names of the HudIndicator enum entries
    // Default value ensures new robots start with all indicators visible
    val enabledIndicators: List<String> = HudIndicator.entries.map { it.name }
)