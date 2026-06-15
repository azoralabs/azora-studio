package dev.azora.sdk.core.project.domain

import kotlinx.serialization.json.*

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val KEY_GLOBAL_CONSTANTS = "globalConstants"

/**
 * Gets the global constants from project settings.
 */
val ProjectSettings.globalConstants: List<GlobalConstant>
    get() {
        val element = extras[KEY_GLOBAL_CONSTANTS] ?: return emptyList()
        return try {
            json.decodeFromJsonElement<List<GlobalConstant>>(element)
        } catch (e: Exception) {
            emptyList()
        }
    }

/**
 * Returns a copy of settings with updated global constants.
 */
fun ProjectSettings.withGlobalConstants(constants: List<GlobalConstant>): ProjectSettings {
    val newExtras = JsonObject(extras.toMutableMap().apply {
        put(KEY_GLOBAL_CONSTANTS, json.encodeToJsonElement(constants))
    })
    return copy(extras = newExtras)
}
