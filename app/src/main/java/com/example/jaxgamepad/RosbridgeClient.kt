package com.example.jaxgamepad

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class RosTopicInfo(
    val name: String,
    val type: String
)

data class DiscoveredRobotTopics(
    val allTopics: List<RosTopicInfo>,
    val cmdVelTopic: TopicBinding? = null,
    val modeTopic: TopicBinding? = null,
    val batteryTopic: TopicBinding? = null,
    val imuTopic: TopicBinding? = null,
    val odomTopic: TopicBinding? = null,
    val jointStateTopic: TopicBinding? = null
)

class RosbridgeClient {
    private val client by lazy { OkHttpClient() }
    private var socket: WebSocket? = null

    private val pendingServiceCalls =
        ConcurrentHashMap<String, (success: Boolean, values: JSONObject?) -> Unit>()

    private val activeSubscriptions =
        mutableMapOf<String, (JSONObject) -> Unit>()

    var isConnected by mutableStateOf(false)
        private set

    var statusText by mutableStateOf("Disconnected")
        private set

    var lastBatteryPercent by mutableStateOf<Int?>(null)
        private set

    var lastModeText by mutableStateOf<String?>(null)
        private set

    fun connect(url: String, onConnected: (() -> Unit)? = null) {
        if (isConnected) return

        statusText = "Connecting..."

        val request = Request.Builder()
            .url(url)
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                statusText = "Connected"
                onConnected?.invoke()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                statusText = "Closing"
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                statusText = "Disconnected"
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                statusText = "Failed: ${t.message ?: "Unknown error"}"
            }
        })
    }

    fun disconnect() {
        activeSubscriptions.keys.toList().forEach { topic ->
            unsubscribe(topic)
        }

        socket?.close(1000, "bye")
        socket = null
        isConnected = false
        statusText = "Disconnected"
        pendingServiceCalls.clear()
    }

    fun advertiseIfNeeded(robot: RobotConfig) {
        advertiseTopic(robot.cmdVelTopic)
        advertiseTopic(robot.modeTopic)
    }

    private fun advertiseTopic(binding: TopicBinding?) {
        if (!isConnected || binding == null) return
        if (binding.name.isBlank() || binding.type.isBlank()) return

        val msg = JSONObject().apply {
            put("op", "advertise")
            put("topic", binding.name)
            put("type", binding.type)
        }

        socket?.send(msg.toString())
    }

    fun publishCmdVel(
        robot: RobotConfig,
        linearX: Double,
        linearY: Double,
        angularZ: Double
    ) {
        val binding = robot.cmdVelTopic ?: return
        if (!isConnected) return

        val msg = JSONObject().apply {
            put("op", "publish")
            put("topic", binding.name)
            put(
                "msg",
                JSONObject().apply {
                    put(
                        "linear",
                        JSONObject().apply {
                            put("x", linearX)
                            put("y", linearY)
                            put("z", 0.0)
                        }
                    )
                    put(
                        "angular",
                        JSONObject().apply {
                            put("x", 0.0)
                            put("y", 0.0)
                            put("z", angularZ)
                        }
                    )
                }
            )
        }

        socket?.send(msg.toString())
    }

    fun publishMode(robot: RobotConfig, mode: String) {
        val binding = robot.modeTopic ?: return
        if (!isConnected) return

        val msg = JSONObject().apply {
            put("op", "publish")
            put("topic", binding.name)
            put(
                "msg",
                JSONObject().apply {
                    put("data", mode)
                }
            )
        }

        socket?.send(msg.toString())
    }

    fun subscribeToTelemetry(robot: RobotConfig) {
        robot.batteryTopic?.let { binding ->
            subscribe(binding.name, binding.type) { msg ->
                val percentage = when {
                    msg.has("percentage") -> {
                        val raw = msg.optDouble("percentage", Double.NaN)
                        if (raw.isNaN()) null else (raw * 100.0).toInt().coerceIn(0, 100)
                    }
                    msg.has("capacity") -> {
                        val raw = msg.optDouble("capacity", Double.NaN)
                        if (raw.isNaN()) null else raw.toInt().coerceIn(0, 100)
                    }
                    else -> null
                }

                lastBatteryPercent = percentage
            }
        }

        robot.modeTopic?.let { binding ->
            subscribe(binding.name, binding.type) { msg ->
                lastModeText = msg.optString("data", null)
            }
        }
    }

    fun clearTelemetrySubscriptions(robot: RobotConfig) {
        robot.batteryTopic?.let { unsubscribe(it.name) }
        robot.modeTopic?.let { unsubscribe(it.name) }
        robot.imuTopic?.let { unsubscribe(it.name) }
        robot.odomTopic?.let { unsubscribe(it.name) }
        robot.jointStateTopic?.let { unsubscribe(it.name) }
    }

    fun subscribe(
        topic: String,
        type: String,
        onMessage: (JSONObject) -> Unit
    ) {
        if (!isConnected) return

        activeSubscriptions[topic] = onMessage

        val msg = JSONObject().apply {
            put("op", "subscribe")
            put("topic", topic)
            put("type", type)
            put("queue_length", 1)
            put("throttle_rate", 100)
        }

        socket?.send(msg.toString())
    }

    fun unsubscribe(topic: String) {
        if (!isConnected) return

        activeSubscriptions.remove(topic)

        val msg = JSONObject().apply {
            put("op", "unsubscribe")
            put("topic", topic)
        }

        socket?.send(msg.toString())
    }

    fun discoverTopics(onResult: (Result<DiscoveredRobotTopics>) -> Unit) {
        if (!isConnected) {
            onResult(Result.failure(IllegalStateException("Not connected")))
            return
        }

        callService("/rosapi/topics") { topicsResult ->
            topicsResult
                .onFailure { onResult(Result.failure(it)) }
                .onSuccess { topicNamesResponse ->
                    callService("/rosapi/topic_types") { typesResult ->
                        typesResult
                            .onFailure { onResult(Result.failure(it)) }
                            .onSuccess { topicTypesResponse ->
                                try {
                                    val allTopics = mergeTopicsAndTypes(
                                        topicsResponse = topicNamesResponse,
                                        topicTypesResponse = topicTypesResponse
                                    )
                                    val suggestions = autoDetectTopics(allTopics)
                                    onResult(Result.success(suggestions))
                                } catch (e: Exception) {
                                    onResult(Result.failure(e))
                                }
                            }
                    }
                }
        }
    }

    private fun mergeTopicsAndTypes(
        topicsResponse: JSONObject,
        topicTypesResponse: JSONObject
    ): List<RosTopicInfo> {
        val topicNamesArray = topicsResponse.optJSONArray("topics") ?: JSONArray()
        val topicNameList = mutableListOf<String>()

        for (i in 0 until topicNamesArray.length()) {
            val topicName = topicNamesArray.optString(i)
            if (topicName.isNotBlank()) {
                topicNameList.add(topicName)
            }
        }

        val topicTypesArray = topicTypesResponse.optJSONArray("topics") ?: JSONArray()
        val typesArray = topicTypesResponse.optJSONArray("types") ?: JSONArray()

        val typeMap = mutableMapOf<String, String>()
        val count = minOf(topicTypesArray.length(), typesArray.length())

        for (i in 0 until count) {
            val name = topicTypesArray.optString(i)
            val type = typesArray.optString(i)
            if (name.isNotBlank() && type.isNotBlank()) {
                typeMap[name] = type
            }
        }

        return topicNameList
            .map { topicName ->
                RosTopicInfo(
                    name = topicName,
                    type = typeMap[topicName].orEmpty()
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun autoDetectTopics(allTopics: List<RosTopicInfo>): DiscoveredRobotTopics {
        fun findByExactType(type: String): TopicBinding? {
            val match = allTopics.firstOrNull { it.type == type }
            return match?.toBinding()
        }

        fun findByNameHintAndType(nameHint: String, type: String): TopicBinding? {
            val match = allTopics.firstOrNull {
                it.type == type && it.name.contains(nameHint, ignoreCase = true)
            }
            return match?.toBinding()
        }

        val cmdVelTopic =
            findByNameHintAndType("cmd_vel", "geometry_msgs/Twist")
                ?: findByExactType("geometry_msgs/Twist")

        val modeTopic =
            findByNameHintAndType("mode", "std_msgs/String")
                ?: allTopics.firstOrNull {
                    it.type == "std_msgs/String" &&
                            (
                                    it.name.contains("mode", ignoreCase = true) ||
                                            it.name.contains("state", ignoreCase = true)
                                    )
                }?.toBinding()

        val batteryTopic =
            findByExactType("sensor_msgs/BatteryState")
                ?: allTopics.firstOrNull {
                    it.name.contains("battery", ignoreCase = true) ||
                            it.name.contains("power", ignoreCase = true)
                }?.toBinding()

        val imuTopic =
            findByExactType("sensor_msgs/Imu")
                ?: allTopics.firstOrNull {
                    it.name.contains("imu", ignoreCase = true)
                }?.toBinding()

        val odomTopic =
            findByExactType("nav_msgs/Odometry")
                ?: allTopics.firstOrNull {
                    it.name.contains("odom", ignoreCase = true)
                }?.toBinding()

        val jointStateTopic =
            findByExactType("sensor_msgs/JointState")
                ?: allTopics.firstOrNull {
                    it.name.contains("joint_states", ignoreCase = true)
                }?.toBinding()

        return DiscoveredRobotTopics(
            allTopics = allTopics,
            cmdVelTopic = cmdVelTopic,
            modeTopic = modeTopic,
            batteryTopic = batteryTopic,
            imuTopic = imuTopic,
            odomTopic = odomTopic,
            jointStateTopic = jointStateTopic
        )
    }

    private fun RosTopicInfo.toBinding(): TopicBinding {
        return TopicBinding(name = name, type = type)
    }

    private fun callService(
        service: String,
        callback: (Result<JSONObject>) -> Unit
    ) {
        if (!isConnected) {
            callback(Result.failure(IllegalStateException("Not connected")))
            return
        }

        val id = UUID.randomUUID().toString()

        pendingServiceCalls[id] = { success, values ->
            if (!success || values == null) {
                callback(Result.failure(IllegalStateException("Service call failed: $service")))
            } else {
                callback(Result.success(values))
            }
        }

        val msg = JSONObject().apply {
            put("op", "call_service")
            put("id", id)
            put("service", service)
            put("args", JSONObject())
        }

        socket?.send(msg.toString())
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)

            when (json.optString("op")) {
                "service_response" -> {
                    val id = json.optString("id")
                    val result = json.optBoolean("result", false)
                    val values = json.optJSONObject("values")
                    pendingServiceCalls.remove(id)?.invoke(result, values)
                }

                "publish" -> {
                    val topic = json.optString("topic")
                    val msg = json.optJSONObject("msg") ?: return
                    activeSubscriptions[topic]?.invoke(msg)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}