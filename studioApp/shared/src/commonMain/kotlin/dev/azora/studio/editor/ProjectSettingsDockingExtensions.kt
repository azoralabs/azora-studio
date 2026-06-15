package dev.azora.studio.editor

import dev.azora.sdk.core.project.domain.ProjectSettings
import dev.azora.sdk.docking.domain.DockLayout
import kotlinx.serialization.json.*

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val KEY_DOCK_LAYOUT = "dockLayout"

/**
 * Gets the dock layout persisted in the project settings (stored in [ProjectSettings.extras]).
 */
val ProjectSettings.dockLayout: DockLayout?
    get() {
        val element = extras[KEY_DOCK_LAYOUT] ?: return null
        return try {
            json.decodeFromJsonElement<DockLayout>(element)
        } catch (e: Exception) {
            null
        }
    }

/**
 * Returns a copy of settings with the dock layout updated (or removed when null).
 */
fun ProjectSettings.withDockLayout(layout: DockLayout?): ProjectSettings {
    val newExtras = JsonObject(extras.toMutableMap().apply {
        if (layout != null) {
            put(KEY_DOCK_LAYOUT, json.encodeToJsonElement(layout))
        } else {
            remove(KEY_DOCK_LAYOUT)
        }
    })
    return copy(extras = newExtras)
}
