package com.example.jaxgamepad

import android.content.Context
import android.net.Uri
import com.example.jaxgamepad.ui.screens.TopicDropdownItem
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "robot_thumb_${UUID.randomUUID()}.jpg")
        val outputStream = FileOutputStream(file)

        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun formatUptime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

fun formatDistance(meters: Double): String {
    return if (meters >= 1000.0) {
        String.format("%.2f km", meters / 1000.0)
    } else {
        String.format("%.1f m", meters)
    }
}

fun getNetworkDetails(context: Context): Pair<String, String> {
    val ipAddress = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .filter { !it.isLoopbackAddress && it is Inet4Address }
            .map { it.hostAddress ?: "0.0.0.0" }
            .firstOrNull { it.startsWith("192.") } ?: "0.0.0.0"
    } catch (e: Exception) {
        "0.0.0.0"
    }

    val status = if (ipAddress.startsWith("192.")) {
        "WIFI_CONNECTED"
    } else {
        "WIFI_NOT_CONNECTED"
    }

    return status to ipAddress
}

fun buildTopicOptions(
    allTopics: List<RosTopicInfo>,
    preferredTypes: List<String>,
    standardOption: TopicBinding? = null
): List<TopicDropdownItem> {
    val result = mutableListOf<TopicDropdownItem>()

    if (standardOption != null) {
        result += TopicDropdownItem(label = "Standard", isHeader = true)
        result += TopicDropdownItem(binding = standardOption)
    }

    val preferred = allTopics
        .filter { it.type in preferredTypes }
        .sortedBy { it.name.lowercase() }
        .map { TopicDropdownItem(binding = TopicBinding(it.name, it.type)) }

    val others = allTopics
        .filter { it.type !in preferredTypes }
        .sortedBy { it.name.lowercase() }
        .map {
            TopicDropdownItem(
                binding = TopicBinding(
                    it.name,
                    if (it.type.isBlank()) "unknown" else it.type
                )
            )
        }

    if (preferred.isNotEmpty()) {
        result += TopicDropdownItem(label = "Recommended", isHeader = true)
        result += preferred
    }

    if (others.isNotEmpty()) {
        result += TopicDropdownItem(label = "All discovered topics", isHeader = true)
        result += others
    }

    return result.distinctBy {
        if (it.isHeader) {
            "header:${it.label}"
        } else {
            "${it.binding?.name}|${it.binding?.type}"
        }
    }
}