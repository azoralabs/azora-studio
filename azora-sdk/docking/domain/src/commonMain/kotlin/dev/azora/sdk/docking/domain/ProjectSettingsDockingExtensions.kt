package dev.azora.sdk.docking.domain

import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Serializes this dock layout to a JSON string suitable for persistence
 * (e.g. inside a project file or preferences entry).
 */
fun DockLayout.toJson(): String =
    json.encodeToString(DockLayout.serializer(), this)

/**
 * Parses a [DockLayout] from a JSON string previously produced by [toJson].
 * Returns `null` if the value is missing or cannot be decoded.
 */
fun dockLayoutFromJson(value: String?): DockLayout? {
    if (value.isNullOrBlank()) return null
    return try {
        json.decodeFromString(DockLayout.serializer(), value)
    } catch (e: Exception) {
        null
    }
}
