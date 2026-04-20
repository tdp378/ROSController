package com.jax.roscontroller

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
    val cpuTempTopic: TopicBinding? = null,
    val odomTopic: TopicBinding? = null,
    val jointStateTopic: TopicBinding? = null,
    val footSensorsTopic: TopicBinding? = null
)

class RosbridgeClient {
    private val client by lazy { OkHttpClient() }
    private var socket: WebSocket? = null

    private val pendingServiceCalls =
        ConcurrentHashMap<String, (success: Boolean, values: JSONObject?) -> Unit>()

    private val activeSubscriptions =
        mutableMapOf<String, (JSONObject) -> Unit>()

    private fun normalizeTopic(topic: String): String {
        return if (topic.startsWith("/")) topic else "/$topic"
    }

    var isConnected by mutableStateOf(false)
        private set

    var statusText by mutableStateOf("Disconnected")
        private set

    var lastBatteryPercent by mutableStateOf<Int?>(null)
        private set

    var lastBatteryVoltage by mutableStateOf<Double?>(null)
        private set

    var lastModeText by mutableStateOf<String?>(null)
        private set

    var isImuActive by mutableStateOf(false)
        private set

    var isOdomActive by mutableStateOf(false)
        private set

    var isFootSensorsActive by mutableStateOf(false)
        private set

    var totalDistance by mutableStateOf(0.0)
        private set

    private var lastX: Double? = null
    private var lastY: Double? = null

    var lastCpuTemp by mutableStateOf<Int?>(null)
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
                resetTelemetry()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                statusText = "Disconnected"
                resetTelemetry()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                statusText = "Failed: ${t.message ?: "Unknown error"}"
                resetTelemetry()
            }
        })
    }

    private fun resetTelemetry() {
        isImuActive = false
        isOdomActive = false
        lastX = null
        lastY = null
        lastBatteryPercent = null
        lastModeText = null
        lastCpuTemp = null
        activeSubscriptions.clear()
    }

    fun disconnect() {
        activeSubscriptions.keys.toList().forEach { topic ->
            unsubscribe(topic)
        }

        socket?.close(1000, "bye")
        socket = null
        isConnected = false
        statusText = "Disconnected"
        resetTelemetry()
        totalDistance = 0.0
        pendingServiceCalls.clear()
    }

    fun advertiseIfNeeded(robot: RobotConfig) {
        advertiseTopic(robot.cmdVelTopic)
        advertiseTopic(robot.modeTopic)
    }

    private fun advertiseTopic(binding: TopicBinding?) {
        if (!isConnected || binding == null) return
        if (binding.name.isBlank() || binding.type.isBlank()) return

        val normalizedName = normalizeTopic(binding.name)

        val msg = JSONObject().apply {
            put("op", "advertise")
            put("topic", normalizedName)
            put("type", binding.type)
        }

        socket?.send(msg.toString())
    }

    fun publishCmdVel(
        robot: RobotConfig,
        linearX: Double,
        linearY: Double,
        linearZ: Double,
        angularX: Double,
        angularY: Double,
        angularZ: Double
    ) {
        val binding = robot.cmdVelTopic ?: return
        if (!isConnected) return

        val msg = JSONObject().apply {
            put("op", "publish")
            put("topic", normalizeTopic(binding.name))
            put(
                "msg",
                JSONObject().apply {
                    put(
                        "linear",
                        JSONObject().apply {
                            put("x", linearX)
                            put("y", linearY)
                            put("z", linearZ)
                        }
                    )
                    put(
                        "angular",
                        JSONObject().apply {
                            put("x", angularX)
                            put("y", angularY)
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
            put("topic", normalizeTopic(binding.name))
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
        // Reset state for new robot
        isImuActive = false
        isOdomActive = false
        isFootSensorsActive = false

        robot.batteryTopic?.let { binding ->
            subscribe(binding.name, binding.type) { msg ->
                if (msg.has("voltage")) {
                    lastBatteryVoltage = msg.optDouble("voltage")
                }

                val percentage = when {
                    msg.has("percentage") -> {
                        val raw = msg.optDouble("percentage", Double.NaN)
                        // If percentage is 0.0, we'll keep it as 0, but HUD will now show voltage as fallback
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

        robot.imuTopic?.let { binding ->
            subscribe(binding.name, binding.type) { _ ->
                isImuActive = true
            }
        }

        robot.odomTopic?.let { binding ->
            subscribe(binding.name, binding.type) { msg ->
                isOdomActive = true
                
                // Support both nav_msgs/Odometry (pose.pose.position) and geometry_msgs/PoseStamped (pose.position)
                val pose = msg.optJSONObject("pose")
                val position = if (pose != null) {
                    if (pose.has("pose")) {
                        // nav_msgs/Odometry structure
                        pose.optJSONObject("pose")?.optJSONObject("position")
                    } else {
                        // geometry_msgs/PoseStamped structure or direct position
                        pose.optJSONObject("position")
                    }
                } else {
                    null
                }

                if (position != null) {
                    val x = position.optDouble("x", Double.NaN)
                    val y = position.optDouble("y", Double.NaN)

                    if (!x.isNaN() && !y.isNaN()) {
                        if (lastX != null && lastY != null) {
                            val dx = x - lastX!!
                            val dy = y - lastY!!
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                            
                            // Log small movements for diagnostics
                            if (distance > 0.001) {
                                android.util.Log.d("RosbridgeClient", "Odom movement: dx=${"%.4f".format(dx)}, dy=${"%.4f".format(dy)}, dist=${"%.4f".format(distance)}")
                            }

                            if (distance < 5.0) { // Filter out jumps (e.g., on reset)
                                if (distance > 0.001) { // deadband
                                    totalDistance += distance
                                }
                            } else {
                                android.util.Log.d("RosbridgeClient", "Ignored large odom jump: $distance")
                            }
                        }
                        lastX = x
                        lastY = y
                    }
                } else {
                    android.util.Log.w("RosbridgeClient", "Received odom but could not find position in: $msg")
                }
            }
        }

        robot.cpuTempTopic?.let { binding ->
            subscribe(binding.name, binding.type) { msg ->
                val temp = when {
                    msg.has("data") -> msg.optDouble("data", 0.0)
                    msg.has("temperature") -> msg.optDouble("temperature", 0.0)
                    else -> 0.0
                }
                lastCpuTemp = temp.toInt()
            }
        }

        robot.footSensorsTopic?.let { binding ->
            subscribe(binding.name, binding.type) { _ ->
                isFootSensorsActive = true
            }
        }
    }

    fun clearTelemetrySubscriptions(robot: RobotConfig) {
        robot.batteryTopic?.let { unsubscribe(it.name) }
        robot.modeTopic?.let { unsubscribe(it.name) }
        robot.imuTopic?.let { unsubscribe(it.name) }
        robot.cpuTempTopic?.let { unsubscribe(it.name) }
        robot.odomTopic?.let { unsubscribe(it.name) }
        robot.jointStateTopic?.let { unsubscribe(it.name) }
    }

    fun subscribe(
        topic: String,
        type: String,
        onMessage: (JSONObject) -> Unit
    ) {
        if (!isConnected) return
        val normalizedTopic = normalizeTopic(topic)

        // Prevent redundant subscriptions
        if (activeSubscriptions.containsKey(normalizedTopic)) {
            android.util.Log.d("RosbridgeClient", "Already subscribed to $normalizedTopic, updating callback.")
            activeSubscriptions[normalizedTopic] = onMessage
            return
        }

        activeSubscriptions[normalizedTopic] = onMessage

        val msg = JSONObject().apply {
            put("op", "subscribe")
            put("topic", normalizedTopic)
            put("type", type)
            put("queue_length", 1)
            put("throttle_rate", 100)
        }

        socket?.send(msg.toString())
    }

    fun unsubscribe(topic: String) {
        val normalizedTopic = normalizeTopic(topic)
        activeSubscriptions.remove(normalizedTopic)

        if (!isConnected) return

        val msg = JSONObject().apply {
            put("op", "unsubscribe")
            put("topic", normalizedTopic)
        }

        socket?.send(msg.toString())
    }

    fun discoverTopics(
        nameHint: String? = null,
        onResult: (Result<DiscoveredRobotTopics>) -> Unit
    ) {
        if (!isConnected) {
            onResult(Result.failure(IllegalStateException("Not connected")))
            return
        }

        callService("/rosapi/topics") { topicsResult ->
            topicsResult
                .onFailure { onResult(Result.failure(it)) }
                .onSuccess { topicNamesResponse ->
                    callService("/rosapi/topics_and_raw_types") { typesResult ->
                        typesResult
                            .onFailure { onResult(Result.failure(it)) }
                            .onSuccess { topicTypesResponse ->
                                try {
                                    val allTopics = mergeTopicsAndTypes(
                                        topicsResponse = topicNamesResponse,
                                        topicTypesResponse = topicTypesResponse
                                    )
                                    val suggestions = autoDetectTopics(allTopics, nameHint)
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
            val rawTypeEntry = typesArray.opt(i)

            val resolvedType = when (rawTypeEntry) {
                is JSONArray -> {
                    if (rawTypeEntry.length() > 0) rawTypeEntry.optString(0) else ""
                }
                is String -> rawTypeEntry
                else -> ""
            }

            if (name.isNotBlank() && resolvedType.isNotBlank()) {
                typeMap[name] = resolvedType
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

    private fun autoDetectTopics(
        allTopics: List<RosTopicInfo>,
        robotNameHint: String? = null
    ): DiscoveredRobotTopics {
        val sanitizedHint = robotNameHint?.trim()?.lowercase()?.replace(" ", "_")

        fun findByExactType(type: String): TopicBinding? {
            // If we have a hint, prefer topics containing the hint
            if (!sanitizedHint.isNullOrBlank()) {
                val matchWithHint = allTopics.firstOrNull { 
                    it.type == type && it.name.lowercase().contains(sanitizedHint) 
                }
                if (matchWithHint != null) return matchWithHint.toBinding()
            }
            
            val match = allTopics.firstOrNull { it.type == type }
            return match?.toBinding()
        }

        fun findByNameHintAndType(nameHint: String, type: String): TopicBinding? {
            // If we have a robot name hint, try matching BOTH robot name and topic hint
            if (!sanitizedHint.isNullOrBlank()) {
                val bestMatch = allTopics.firstOrNull {
                    it.type == type && 
                    it.name.lowercase().contains(sanitizedHint) && 
                    it.name.lowercase().contains(nameHint.lowercase())
                }
                if (bestMatch != null) return bestMatch.toBinding()
            }

            val match = allTopics.firstOrNull {
                it.type == type && it.name.contains(nameHint, ignoreCase = true)
            }
            return match?.toBinding()
        }

        val cmdVelTopic =
            findByNameHintAndType("cmd_vel", "geometry_msgs/msg/Twist")
                ?: findByExactType("geometry_msgs/msg/Twist")

        val modeTopic =
            findByNameHintAndType("mode", "std_msgs/msg/String")
                ?: allTopics.firstOrNull {
                    val isStringType = it.type == "std_msgs/msg/String"
                    val hasModeOrState = it.name.contains("mode", ignoreCase = true) || it.name.contains("state", ignoreCase = true)
                    val matchesRobotHint = sanitizedHint == null || it.name.lowercase().contains(sanitizedHint)
                    
                    isStringType && hasModeOrState && matchesRobotHint
                }?.toBinding()
                ?: allTopics.firstOrNull {
                    it.type == "std_msgs/msg/String" &&
                    (it.name.contains("mode", ignoreCase = true) || it.name.contains("state", ignoreCase = true))
                }?.toBinding()

        val batteryTopic =
            findByExactType("sensor_msgs/msg/BatteryState")
                ?: allTopics.firstOrNull {
                    val isBattery = it.name.contains("battery", ignoreCase = true) || it.name.contains("power", ignoreCase = true)
                    val matchesRobotHint = sanitizedHint == null || it.name.lowercase().contains(sanitizedHint)
                    isBattery && matchesRobotHint
                }?.toBinding()
                ?: allTopics.firstOrNull {
                    it.name.contains("battery", ignoreCase = true) || it.name.contains("power", ignoreCase = true)
                }?.toBinding()

        val imuTopic =
            findByExactType("sensor_msgs/msg/Imu")
                ?: allTopics.firstOrNull {
                    val isImu = it.name.contains("imu", ignoreCase = true)
                    val matchesRobotHint = sanitizedHint == null || it.name.lowercase().contains(sanitizedHint)
                    isImu && matchesRobotHint
                }?.toBinding()
                ?: allTopics.firstOrNull {
                    it.name.contains("imu", ignoreCase = true)
                }?.toBinding()

        val cpuTempTopic =
            allTopics.firstOrNull {
                val isTempType = it.type == "std_msgs/msg/Float32" || it.type == "sensor_msgs/msg/Temperature"
                val hasTempHint = it.name.contains("cpu", ignoreCase = true) || it.name.contains("temp", ignoreCase = true)
                val matchesRobotHint = sanitizedHint == null || it.name.lowercase().contains(sanitizedHint)
                isTempType && hasTempHint && matchesRobotHint
            }?.toBinding()
            ?: allTopics.firstOrNull {
                (it.name.contains("cpu", ignoreCase = true) || it.name.contains("temp", ignoreCase = true)) &&
                        (it.type == "std_msgs/msg/Float32" || it.type == "sensor_msgs/msg/Temperature")
            }?.toBinding()

        val odomTopic =
            findByExactType("nav_msgs/msg/Odometry")
                ?: allTopics.firstOrNull {
                    val isOdom = it.name.contains("odom", ignoreCase = true)
                    val matchesRobotHint = sanitizedHint == null || it.name.lowercase().contains(sanitizedHint)
                    isOdom && matchesRobotHint
                }?.toBinding()
                ?: allTopics.firstOrNull {
                    it.name.contains("odom", ignoreCase = true)
                }?.toBinding()

        val jointStateTopic =
            findByExactType("sensor_msgs/msg/JointState")
                ?: allTopics.firstOrNull {
                    val isJoint = it.name.contains("joint_states", ignoreCase = true)
                    val matchesRobotHint = sanitizedHint == null || it.name.lowercase().contains(sanitizedHint)
                    isJoint && matchesRobotHint
                }?.toBinding()
                ?: allTopics.firstOrNull {
                    it.name.contains("joint_states", ignoreCase = true)
                }?.toBinding()

        val footSensorsTopic =
            allTopics.firstOrNull {
                val isMultiArray = it.type == "std_msgs/msg/Float32MultiArray" || it.type == "std_msgs/msg/Int32MultiArray"
                val hasFootHint = it.name.contains("foot", ignoreCase = true) || it.name.contains("sensor", ignoreCase = true)
                val matchesRobotHint = sanitizedHint == null || it.name.lowercase().contains(sanitizedHint)
                isMultiArray && hasFootHint && matchesRobotHint
            }?.toBinding()
            ?: allTopics.firstOrNull {
                (it.name.contains("foot", ignoreCase = true) || it.name.contains("sensor", ignoreCase = true)) &&
                        (it.type == "std_msgs/msg/Float32MultiArray" || it.type == "std_msgs/msg/Int32MultiArray")
            }?.toBinding()

        return DiscoveredRobotTopics(
            allTopics = allTopics,
            cmdVelTopic = cmdVelTopic,
            modeTopic = modeTopic,
            batteryTopic = batteryTopic,
            imuTopic = imuTopic,
            cpuTempTopic = cpuTempTopic,
            odomTopic = odomTopic,
            jointStateTopic = jointStateTopic,
            footSensorsTopic = footSensorsTopic
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
                    val normalizedTopic = normalizeTopic(topic)
                    val msg = json.optJSONObject("msg") ?: return
                    activeSubscriptions[normalizedTopic]?.invoke(msg)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

