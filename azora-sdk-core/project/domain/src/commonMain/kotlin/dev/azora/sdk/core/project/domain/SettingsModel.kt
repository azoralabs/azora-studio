package dev.azora.sdk.core.project.domain

import kotlinx.serialization.Serializable

/**
 * Color picker mode options for the settings.
 */
enum class ColorPickerMode {
    Triangle,
    Sliders
}

/**
 * A color saved in the project palette.
 *
 * @param id Unique identifier for the color
 * @param name Display name for the color
 * @param hex Hex color value (e.g., "#FFFFFFFF" for white, "#00000000" for transparent)
 * @param isDeletable Whether the color can be deleted from the palette
 * @param isEditable Whether the color's hex value can be modified
 */
@Serializable
data class PaletteColor(
    val id: String = generateId(),
    val name: String,
    val hex: String,
    val isDeletable: Boolean = true,
    val isEditable: Boolean = true
) {

    companion object {

        private fun generateId(): String = kotlin.random.Random.nextLong().toString(36)

        /** Default palette colors for new projects */
        val defaults: List<PaletteColor> = listOf(
            PaletteColor(id = "white", name = "White", hex = "#FFFFFFFF", isDeletable = false, isEditable = false),
            PaletteColor(id = "black", name = "Black", hex = "#FF000000", isDeletable = false, isEditable = false),
            PaletteColor(id = "transparent", name = "Transparent", hex = "#00000000", isDeletable = false, isEditable = false),
            PaletteColor(id = "neutral", name = "Neutral", hex = "#FF818181", isDeletable = false, isEditable = false),
            PaletteColor(id = "primary", name = "Primary", hex = "#FFD14EEA", isDeletable = false, isEditable = true),
            PaletteColor(id = "secondary", name = "Secondary", hex = "#FF4E93EA", isDeletable = false, isEditable = true)
        )
    }
}

/**
 * Project settings model containing user preferences.
 */
@Serializable
data class SettingsModel(
    val projectId: String,
    val editorTooltipsEnabled: Boolean = true,
    val tooltipDelaySeconds: Int = 1,
    val preferredColorPicker: ColorPickerMode = ColorPickerMode.Triangle,
    val paletteColors: List<PaletteColor> = emptyList(),
    val globalConstants: List<GlobalConstant> = emptyList(),
    val azScriptUsePastel: Boolean = true,
    val azScriptBoldKeywords: Boolean = true,
    val azScriptItalicPreprocessor: Boolean = true,
    val azScriptUnderlineVariables: Boolean = true,
    val azoraLangPath: String = "",
    val showRuntimeWarnings: Boolean = false
) {

    companion object {

        fun default(projectId: String) = SettingsModel(projectId = projectId)
    }
}