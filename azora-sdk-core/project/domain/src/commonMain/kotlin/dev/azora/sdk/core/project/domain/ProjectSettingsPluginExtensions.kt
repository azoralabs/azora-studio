package dev.azora.sdk.core.project.domain

import kotlinx.serialization.json.*

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val KEY_ENABLED_PLUGINS = "enabledPluginIds"

/**
 * Plugin ids enabled for this project, or `null` when the project predates
 * per-project plugin enablement (legacy projects keep every installed plugin
 * active). New projects always record an explicit list — empty for the Empty
 * template, the contributing plugin for plugin-provided templates.
 */
val ProjectSettings.enabledPluginIds: List<String>?
    get() {
        val element = extras[KEY_ENABLED_PLUGINS] ?: return null
        return try {
            json.decodeFromJsonElement<List<String>>(element)
        } catch (e: Exception) {
            null
        }
    }

/** Returns a copy of settings with the project's enabled plugin ids. */
fun ProjectSettings.withEnabledPluginIds(ids: List<String>): ProjectSettings {
    val newExtras = JsonObject(extras.toMutableMap().apply {
        put(KEY_ENABLED_PLUGINS, json.encodeToJsonElement(ids))
    })
    return copy(extras = newExtras)
}
