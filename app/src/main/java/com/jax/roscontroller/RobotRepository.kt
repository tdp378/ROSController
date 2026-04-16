package com.jax.roscontroller

import android.util.Log
import com.jax.roscontroller.ui.screens.HudIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jax.roscontroller.RobotConfig
import com.jax.roscontroller.RobotManager  
fun saveRobotConfigToFirestore(
    robot: RobotConfig,
    onResult: (String) -> Unit = {}
) {
    if (robot.isDemoRobot()) {
        onResult("DEMO MODE - CLOUD SYNC DISABLED")
        return
    }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid.isNullOrBlank()) {
        onResult("SAVED LOCALLY - SIGN IN TO SYNC")
        return
    }

    try {
        val db = FirebaseFirestore.getInstance()

        val data = hashMapOf(
            "robotId" to robot.robotId,
            "ownerUid" to uid,
            "name" to robot.name,
            "rosAddress" to robot.rosAddress,
            "videoUrl" to robot.videoUrl,
            "thumbnailPath" to robot.thumbnailPath,
            "invertForwardBack" to robot.invertForwardBack,
            "invertStrafe" to robot.invertStrafe,
            "invertHeight" to robot.invertHeight,
            "invertTurn" to robot.invertTurn,
            "enabledIndicators" to robot.enabledIndicators,
            "totalUptimeSeconds" to robot.totalUptimeSeconds,
            "totalDistanceMeters" to robot.totalDistanceMeters,
            "updatedAt" to System.currentTimeMillis(),
            "cmdVelTopic_name" to robot.cmdVelTopic?.name,
            "cmdVelTopic_type" to robot.cmdVelTopic?.type,
            "modeTopic_name" to robot.modeTopic?.name,
            "modeTopic_type" to robot.modeTopic?.type,
            "batteryTopic_name" to robot.batteryTopic?.name,
            "batteryTopic_type" to robot.batteryTopic?.type,
            "imuTopic_name" to robot.imuTopic?.name,
            "imuTopic_type" to robot.imuTopic?.type,
            "cpuTempTopic_name" to robot.cpuTempTopic?.name,
            "cpuTempTopic_type" to robot.cpuTempTopic?.type,
            "odomTopic_name" to robot.odomTopic?.name,
            "odomTopic_type" to robot.odomTopic?.type,
            "jointStateTopic_name" to robot.jointStateTopic?.name,
            "jointStateTopic_type" to robot.jointStateTopic?.type,
            "modes" to robot.modes.map {
                hashMapOf(
                    "label" to it.label,
                    "command" to it.command
                )
            }
        )

        db.collection("users")
            .document(uid)
            .collection("robots")
            .document(robot.robotId)
            .set(data)
            .addOnSuccessListener {
                Log.d("FIRESTORE_ROBOT", "Saved robot: ${robot.name} (${robot.robotId})")
                onResult("- CLOUD SYNC SUCCESSFUL")
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_ROBOT", "Failed saving robot", e)
                onResult("CLOUD SYNC FAILED\n${e.message ?: "Unknown error"}")
            }
    } catch (e: Exception) {
        Log.e("FIRESTORE_ROBOT", "Exception saving robot", e)
        onResult("CLOUD SYNC FAILED\n${e.message ?: "Unknown error"}")
    }
}

fun deleteRobotConfigFromFirestore(
    robot: RobotConfig,
    onResult: (String) -> Unit = {}
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid.isNullOrBlank()) {
        onResult("REMOVED LOCALLY")
        return
    }

    try {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("robots")
            .document(robot.robotId)
            .delete()
            .addOnSuccessListener {
                Log.d("FIRESTORE_ROBOT", "Deleted robot: ${robot.name} (${robot.robotId})")
                onResult("REMOVED LOCAL + CLOUD")
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE_ROBOT", "Failed deleting robot", e)
                onResult("REMOVED LOCAL - CLOUD DELETE FAILED\n${e.message ?: "Unknown error"}")
            }
    } catch (e: Exception) {
        Log.e("FIRESTORE_ROBOT", "Exception deleting robot", e)
        onResult("REMOVED LOCAL - CLOUD DELETE FAILED\n${e.message ?: "Unknown error"}")
    }
}
fun syncRobotsToFirestoreForSignedInUser(robots: List<RobotConfig>) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    // Never sync the demo robot to the cloud
    robots.filterNot { it.isDemoRobot() }.forEach { robot ->
        saveRobotConfigToFirestore(robot.copy(ownerUid = uid))
    }
}

fun fetchRobotsFromFirestoreForSignedInUser(
    uid: String,
    onResult: (List<RobotConfig>) -> Unit,
    onFailure: (Exception) -> Unit = {}
) {
    FirebaseFirestore.getInstance()
        .collection("users")
        .document(uid)
        .collection("robots")
        .get()
        .addOnSuccessListener { snapshot ->
            try {
                val robots = snapshot.documents.mapNotNull { doc ->
                    val robotId = doc.getString("robotId")?.takeIf { it.isNotBlank() } ?: doc.id
                    val name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

                    val modes = (doc.get("modes") as? List<*>)
                        ?.mapNotNull { modeEntry ->
                            val modeMap = modeEntry as? Map<*, *> ?: return@mapNotNull null
                            val label = modeMap["label"] as? String ?: return@mapNotNull null
                            val command = modeMap["command"] as? String ?: return@mapNotNull null
                            if (label.isBlank() || command.isBlank()) null else RobotMode(label, command)
                        }
                        ?: emptyList()

                    val enabledIndicators = (doc.get("enabledIndicators") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.takeIf { it.isNotEmpty() }
                        ?: HudIndicator.entries.map { it.name }

                    fun topicBinding(prefix: String): TopicBinding? {
                        val topicName = doc.getString("${prefix}_name")?.trim().orEmpty()
                        val topicType = doc.getString("${prefix}_type")?.trim().orEmpty()
                        return if (topicName.isNotBlank() && topicType.isNotBlank()) {
                            TopicBinding(name = topicName, type = topicType)
                        } else {
                            null
                        }
                    }

                    RobotConfig(
                        name = name,
                        rosAddress = doc.getString("rosAddress") ?: "",
                        videoUrl = doc.getString("videoUrl") ?: "",
                        thumbnailPath = doc.getString("thumbnailPath"),
                        cmdVelTopic = topicBinding("cmdVelTopic"),
                        modeTopic = topicBinding("modeTopic"),
                        batteryTopic = topicBinding("batteryTopic"),
                        imuTopic = topicBinding("imuTopic"),
                        cpuTempTopic = topicBinding("cpuTempTopic"),
                        odomTopic = topicBinding("odomTopic"),
                        jointStateTopic = topicBinding("jointStateTopic"),
                        modes = modes,
                        enabledIndicators = enabledIndicators,
                        totalUptimeSeconds = doc.getLong("totalUptimeSeconds") ?: 0L,
                        totalDistanceMeters = doc.getDouble("totalDistanceMeters") ?: 0.0,
                        invertForwardBack = doc.getBoolean("invertForwardBack") ?: false,
                        invertStrafe = doc.getBoolean("invertStrafe") ?: false,
                        invertHeight = doc.getBoolean("invertHeight") ?: false,
                        invertTurn = doc.getBoolean("invertTurn") ?: false,
                        robotId = robotId,
                        ownerUid = uid
                    )
                }
                onResult(robots)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

fun buildDemoRobot(ownerUid: String = RobotManager.GUEST_OWNER_UID): RobotConfig {
    return RobotConfig(
        name = "ROSbot (Demo)",
        rosAddress = "192.168.1.XX",
        videoUrl = "http://192.168.1.XX:8080/stream?topic=/camera/image_raw",
        thumbnailPath = "demo_thumb",
        cmdVelTopic = TopicBinding("/cmd_vel", "geometry_msgs/Twist"),
        modeTopic = TopicBinding("/robot_mode", "std_msgs/String"),
        jointStateTopic = TopicBinding("/joint_states", "sensor_msgs/JointState"),
        modes = listOf(
            RobotMode("STAND", "stand"),
            RobotMode("WALK", "walk"),
            RobotMode("LAY", "lay"),
            RobotMode("SHAKE", "shake"),
            RobotMode("SIT", "sit"),
            RobotMode("WAVE", "wave"),
        ),
        invertForwardBack = false,
        invertStrafe = true,
        invertHeight = false,
        invertTurn = true,
        ownerUid = ownerUid
    )
}

fun loadRobotsForOwner(robotManager: RobotManager, ownerUid: String?): List<RobotConfig> {
    val normalizedOwner = RobotManager.normalizeOwnerUid(ownerUid)
    val loaded = robotManager.loadRobots(normalizedOwner)

    if (loaded == null) {
        val demo = buildDemoRobot(normalizedOwner)
        robotManager.saveRobots(listOf(demo), normalizedOwner)
        return listOf(demo)
    }

    // Rule: if there are no other (non-demo) robots, the demo robot must be present.
    val nonDemo = loaded.filterNot { it.isDemoRobot() }
    if (nonDemo.isEmpty()) {
        if (loaded.any { it.isDemoRobot() }) return loaded
        val demo = buildDemoRobot(normalizedOwner)
        robotManager.saveRobots(listOf(demo), normalizedOwner)
        return listOf(demo)
    }

    return loaded
}

