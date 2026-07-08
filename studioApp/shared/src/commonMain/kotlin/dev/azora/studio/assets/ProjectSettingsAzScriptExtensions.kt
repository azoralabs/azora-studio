package dev.azora.studio.assets

import dev.azora.sdk.core.project.domain.ProjectSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

private val azScriptSettingsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val KEY_OPEN_AZ_SCRIPT_FILES = "openAzScriptFiles"

/** Mapping of dock panel id to the `.az` file it edits. */
val ProjectSettings.openAzScriptFiles: Map<String, String>
    get() {
        val element = extras[KEY_OPEN_AZ_SCRIPT_FILES] ?: return emptyMap()
        return runCatching {
            azScriptSettingsJson.decodeFromJsonElement<Map<String, String>>(element)
        }.getOrDefault(emptyMap())
    }

fun ProjectSettings.withOpenAzScriptFiles(mapping: Map<String, String>): ProjectSettings {
    val newExtras = JsonObject(extras.toMutableMap().apply {
        if (mapping.isEmpty()) {
            remove(KEY_OPEN_AZ_SCRIPT_FILES)
        } else {
            put(KEY_OPEN_AZ_SCRIPT_FILES, azScriptSettingsJson.encodeToJsonElement(mapping))
        }
    })
    return copy(extras = newExtras)
}
