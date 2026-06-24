package dev.azora.studio.content_browser

import dev.azora.sdk.core.project.domain.ProjectSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val KEY_OPEN_TEXT_FILES = "openTextFiles"

/**
 * Mapping of panelId -> filePath for text files opened in the Content Browser,
 * persisted in [ProjectSettings.extras] so tabs survive app restarts.
 * Mirrors `openAzoraNodesFiles` in the assets package.
 */
val ProjectSettings.openTextFiles: Map<String, String>
    get() {
        val element = extras[KEY_OPEN_TEXT_FILES] ?: return emptyMap()
        return try {
            json.decodeFromJsonElement<Map<String, String>>(element)
        } catch (e: Exception) {
            emptyMap()
        }
    }

fun ProjectSettings.withOpenTextFiles(mapping: Map<String, String>): ProjectSettings {
    val newExtras = JsonObject(extras.toMutableMap().apply {
        if (mapping.isNotEmpty()) {
            put(KEY_OPEN_TEXT_FILES, json.encodeToJsonElement(mapping))
        } else {
            remove(KEY_OPEN_TEXT_FILES)
        }
    })
    return copy(extras = newExtras)
}
