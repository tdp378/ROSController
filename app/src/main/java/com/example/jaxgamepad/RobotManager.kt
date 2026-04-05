package com.example.jaxgamepad

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RobotManager(context: Context) {
    private val prefs = context.getSharedPreferences("robot_prefs", Context.MODE_PRIVATE)

    fun saveRobots(robots: List<RobotConfig>) {
        val array = JSONArray()

        robots.forEach { robot ->
            val obj = JSONObject().apply {
                put("name", robot.name)
                put("rosAddress", robot.rosAddress)
                put("videoUrl", robot.videoUrl)
                put("thumbnailPath", robot.thumbnailPath)

                put("cmdVelTopic", robot.cmdVelTopic?.toJson())
                put("modeTopic", robot.modeTopic?.toJson())
                put("batteryTopic", robot.batteryTopic?.toJson())
                put("imuTopic", robot.imuTopic?.toJson())
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

                // SAVE INDICATORS
                val indicatorsArray = JSONArray()
                robot.enabledIndicators.forEach { indicatorsArray.put(it) }
                put("enabledIndicators", indicatorsArray)
            }

            array.put(obj)
        }

        prefs.edit().putString("saved_robots", array.toString()).apply()
    }

    fun loadRobots(): List<RobotConfig> {
        val jsonString = prefs.getString("saved_robots", null)
        if (jsonString.isNullOrBlank()) return emptyList()

        val robots = mutableListOf<RobotConfig>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)

                // Load indicators, or default to all if missing
                val indicatorsJson = obj.optJSONArray("enabledIndicators")
                val indicatorsList = if (indicatorsJson != null) {
                    List(indicatorsJson.length()) { indicatorsJson.getString(it) }
                } else {
                    com.example.jaxgamepad.ui.HudIndicator.entries.map { it.name }
                }

                robots.add(
                    RobotConfig(
                        name = obj.optString("name", "Unnamed Robot"),
                        rosAddress = obj.optString("rosAddress", ""),
                        videoUrl = obj.optString("videoUrl", ""),
                        thumbnailPath = obj.optString("thumbnailPath", null),
                        cmdVelTopic = obj.optJSONObject("cmdVelTopic")?.toTopicBinding(),
                        modeTopic = obj.optJSONObject("modeTopic")?.toTopicBinding(),
                        batteryTopic = obj.optJSONObject("batteryTopic")?.toTopicBinding(),
                        imuTopic = obj.optJSONObject("imuTopic")?.toTopicBinding(),
                        odomTopic = obj.optJSONObject("odomTopic")?.toTopicBinding(),
                        jointStateTopic = obj.optJSONObject("jointStateTopic")?.toTopicBinding(),
                        modes = obj.optJSONArray("modes")?.toRobotModes() ?: emptyList(),
                        enabledIndicators = indicatorsList
                    )
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
        return robots
    }

    fun clearAll() { prefs.edit().clear().apply() }
}

private fun TopicBinding.toJson(): JSONObject {
    return JSONObject().apply {
        put("name", name)
        put("type", type)
    }
}

private fun JSONObject.toTopicBinding(): TopicBinding {
    return TopicBinding(name = optString("name", ""), type = optString("type", ""))
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