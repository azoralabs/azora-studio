package dev.azora.sdk.core.project.domain.website

import kotlinx.serialization.Serializable

/**
 * Site-wide visual theme. Colors are `#RRGGBB` hex strings; the generator registers them as Silk
 * base styles / palette overrides.
 */
@Serializable
data class WebTheme(
    val primary: String = "#3B82F6",
    val onPrimary: String = "#FFFFFF",
    val background: String = "#0B0B0F",
    val onBackground: String = "#E5E7EB",
    val surface: String = "#16161D",
    val fontFamily: String = "system-ui, sans-serif"
)
