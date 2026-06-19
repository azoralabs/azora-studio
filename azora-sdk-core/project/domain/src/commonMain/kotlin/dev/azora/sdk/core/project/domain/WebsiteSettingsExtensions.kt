package dev.azora.sdk.core.project.domain

import dev.azora.sdk.core.project.domain.website.WebsiteModel
import kotlinx.serialization.json.*

private val websiteJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val KEY_WEBSITE = "website"

/**
 * The website model stored in this project's settings, or `null` if none has been saved yet
 * (e.g. a non-website project, or before the first save).
 */
val ProjectSettings.website: WebsiteModel?
    get() {
        val element = extras[KEY_WEBSITE] ?: return null
        return try {
            websiteJson.decodeFromJsonElement<WebsiteModel>(element)
        } catch (_: Exception) {
            null
        }
    }

/** Returns a copy of these settings with the website model set to [model]. */
fun ProjectSettings.withWebsite(model: WebsiteModel): ProjectSettings {
    val newExtras = JsonObject(extras.toMutableMap().apply {
        put(KEY_WEBSITE, websiteJson.encodeToJsonElement(model))
    })
    return copy(extras = newExtras)
}
