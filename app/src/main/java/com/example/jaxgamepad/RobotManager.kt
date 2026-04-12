package com.example.jaxgamepad

import android.content.Context
import com.example.jaxgamepad.ui.screens.HudIndicator
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RobotManager(context: Context) {
    private val prefs = context.getSharedPreferences("robot_prefs", Context.MODE_PRIVATE)

    fun saveRobots(
        robots: List<RobotConfig>,
        ownerUid: String = GUEST_OWNER_UID
    ) {
        val normalizedOwner = normalizeOwnerUid(ownerUid)
        val array = JSONArray()

        robots.forEach { originalRobot ->
            val robot = originalRobot.withOwner(normalizedOwner)

            val obj = JSONObject().apply {
                put("name", robot.name)
                put("rosAddress", robot.rosAddress)
                put("videoUrl", robot.videoUrl)
                put("thumbnailPath", robot.thumbnailPath)

                put("cmdVelTopic", robot.cmdVelTopic?.toJson())
                put("modeTopic", robot.modeTopic?.toJson())
                put("batteryTopic", robot.batteryTopic?.toJson())
                put("imuTopic", robot.imuTopic?.toJson())
                put("cpuTempTopic", robot.cpuTempTopic?.toJson())
                put("odomTopic", robot.odomTopic?.toJson())
                put("jointStateTopic", robot.jointStateTopic?.toJson())

                val modesArray = JSONArray()
                robot.modes.forEach { mode ->
                    modesArray.put(
                        JSONObject().apply {
                            put("label", mode.label)
                            put("command", mode.command)
                        }
                    )
                }
                put("modes", modesArray)

                val indicatorsArray = JSONArray()
                robot.enabledIndicators.forEach { indicatorsArray.put(it) }
                put("enabledIndicators", indicatorsArray)

                put("totalUptimeSeconds", robot.totalUptimeSeconds)
                put("totalDistanceMeters", robot.totalDistanceMeters)

                put("invertForwardBack", robot.invertForwardBack)
                put("invertStrafe", robot.invertStrafe)
                put("invertHeight", robot.invertHeight)
                put("invertTurn", robot.invertTurn)

                put("robotId", robot.robotId)
                put("ownerUid", robot.ownerUid)
            }

            array.put(obj)
        }

        prefs.edit().putString(storageKeyForOwner(normalizedOwner), array.toString()).apply()
    }

    fun loadRobots(ownerUid: String = GUEST_OWNER_UID): List<RobotConfig> {
        val normalizedOwner = normalizeOwnerUid(ownerUid)
        val jsonString = when {
            prefs.contains(storageKeyForOwner(normalizedOwner)) ->
                prefs.getString(storageKeyForOwner(normalizedOwner), null)
            normalizedOwner == GUEST_OWNER_UID ->
                prefs.getString(LEGACY_STORAGE_KEY, null)
            else -> null
        }

        if (jsonString.isNullOrBlank()) return emptyList()

        val robots = mutableListOf<RobotConfig>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                val indicatorsJson = obj.optJSONArray("enabledIndicators")
                val indicatorsList = if (indicatorsJson != null) {
                    List(indicatorsJson.length()) { indicatorsJson.getString(it) }
                } else {
                    HudIndicator.entries.map { it.name }
                }

                robots.add(
                    RobotConfig(
                        name = obj.optString("name", "Unnamed Robot"),
                        rosAddress = obj.optString("rosAddress", ""),
                        videoUrl = obj.optString("videoUrl", ""),
                        thumbnailPath = if (obj.isNull("thumbnailPath")) null else obj.optString("thumbnailPath", null),
                        cmdVelTopic = obj.optJSONObject("cmdVelTopic")?.toTopicBinding(),
                        modeTopic = obj.optJSONObject("modeTopic")?.toTopicBinding(),
                        batteryTopic = obj.optJSONObject("batteryTopic")?.toTopicBinding(),
                        imuTopic = obj.optJSONObject("imuTopic")?.toTopicBinding(),
                        cpuTempTopic = obj.optJSONObject("cpuTempTopic")?.toTopicBinding(),
                        odomTopic = obj.optJSONObject("odomTopic")?.toTopicBinding(),
                        jointStateTopic = obj.optJSONObject("jointStateTopic")?.toTopicBinding(),
                        modes = obj.optJSONArray("modes")?.toRobotModes() ?: emptyList(),
                        enabledIndicators = indicatorsList,
                        totalUptimeSeconds = obj.optLong("totalUptimeSeconds", 0L),
                        totalDistanceMeters = obj.optDouble("totalDistanceMeters", 0.0),
                        invertForwardBack = obj.optBoolean("invertForwardBack", false),
                        invertStrafe = obj.optBoolean("invertStrafe", false),
                        invertHeight = obj.optBoolean("invertHeight", false),
                        invertTurn = obj.optBoolean("invertTurn", false),
                        robotId = obj.optString("robotId").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                        ownerUid = obj.optString("ownerUid").takeIf { it.isNotBlank() } ?: normalizedOwner
                    ).withOwner(normalizedOwner)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return robots
    }

    fun mergeGuestRobotsIntoOwner(ownerUid: String): List<RobotConfig> {
        val normalizedOwner = normalizeOwnerUid(ownerUid)
        if (normalizedOwner == GUEST_OWNER_UID) return loadRobots(GUEST_OWNER_UID)

        val guestRobots = loadRobots(GUEST_OWNER_UID)
            .filterNot { it.isDemoRobot() }

        if (guestRobots.isEmpty()) {
            return loadRobots(normalizedOwner)
        }

        val existingUserRobots = loadRobots(normalizedOwner)
        val mergedById = linkedMapOf<String, RobotConfig>()

        existingUserRobots.forEach { robot ->
            mergedById[robot.robotId] = robot.withOwner(normalizedOwner)
        }

        guestRobots.forEach { robot ->
            val ownerRobot = robot.withOwner(normalizedOwner)
            val duplicateByNameId = existingUserRobots.firstOrNull {
                it.name.equals(ownerRobot.name, ignoreCase = true)
            }?.robotId

            val key = duplicateByNameId ?: ownerRobot.robotId
            mergedById[key] = ownerRobot.copy(robotId = key, ownerUid = normalizedOwner)
        }

        val merged = mergedById.values.toList()
        saveRobots(merged, normalizedOwner)
        clearOwner(GUEST_OWNER_UID)
        return merged
    }

    fun clearOwner(ownerUid: String) {
        val normalizedOwner = normalizeOwnerUid(ownerUid)
        val editor = prefs.edit().remove(storageKeyForOwner(normalizedOwner))
        if (normalizedOwner == GUEST_OWNER_UID) {
            editor.remove(LEGACY_STORAGE_KEY)
        }
        editor.apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val GUEST_OWNER_UID = "guest"
        private const val LEGACY_STORAGE_KEY = "saved_robots"

        fun normalizeOwnerUid(ownerUid: String?): String {
            return ownerUid?.takeIf { it.isNotBlank() } ?: GUEST_OWNER_UID
        }

        fun storageKeyForOwner(ownerUid: String?): String {
            return "saved_robots_${normalizeOwnerUid(ownerUid)}"
        }
    }
}

fun RobotConfig.withOwner(ownerUid: String): RobotConfig {
    return copy(ownerUid = RobotManager.normalizeOwnerUid(ownerUid))
}

fun RobotConfig.isDemoRobot(): Boolean {
    return name.equals("ROSbot (Demo)", ignoreCase = true) || thumbnailPath == "demo_thumb"
}

private fun TopicBinding.toJson(): JSONObject {
    return JSONObject().apply {
        put("name", name)
        put("type", type)
    }
}

private fun JSONObject.toTopicBinding(): TopicBinding {
    return TopicBinding(
        name = optString("name", ""),
        type = optString("type", "")
    )
}

private fun JSONArray.toRobotModes(): List<RobotMode> {
    val modes = mutableListOf<RobotMode>()
    for (i in 0 until length()) {
        val obj = optJSONObject(i) ?: continue
        val label = obj.optString("label", "").trim()
        val command = obj.optString("command", "").trim()
        if (label.isNotBlank() && command.isNotBlank()) {
            modes.add(RobotMode(label = label, command = command))
        }
    }
    return modes
}
