package org.azora.studio.assets

import org.azora.sdk.core.project.domain.ProjectSettings
import kotlinx.serialization.json.*

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val KEY_OPEN_AZORA_NODES_FILES = "openAzoraNodesFiles"

/**
 * Gets the mapping of panelId to filePath for open .azn files.
 */
val ProjectSettings.openAzoraNodesFiles: Map<String, String>
    get() {
        val element = extras[KEY_OPEN_AZORA_NODES_FILES] ?: return emptyMap()
        return try {
            json.decodeFromJsonElement<Map<String, String>>(element)
        } catch (e: Exception) {
            emptyMap()
        }
    }

/**
 * Returns a copy of settings with updated open files mapping.
 */
fun ProjectSettings.withOpenAzoraNodesFiles(mapping: Map<String, String>): ProjectSettings {
    val newExtras = JsonObject(extras.toMutableMap().apply {
        if (mapping.isNotEmpty()) {
            put(KEY_OPEN_AZORA_NODES_FILES, json.encodeToJsonElement(mapping))
        } else {
            remove(KEY_OPEN_AZORA_NODES_FILES)
        }
    })
    return copy(extras = newExtras)
}
