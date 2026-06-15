package org.azora.studio.settings

import org.azora.sdk.core.project.domain.*

/**
 * UI state for the settings panel.
 */
data class SettingsState(
    val isLoading: Boolean = true,
    val editorTooltipsEnabled: Boolean = true,
    val tooltipDelaySeconds: Int = 1,
    val preferredColorPicker: ColorPickerMode = ColorPickerMode.Triangle,
    val paletteColors: List<PaletteColor> = emptyList(),
    val useKmpRenderer: Boolean = false,
    val azScriptUsePastel: Boolean = true,
    val azScriptBoldKeywords: Boolean = true,
    val azScriptItalicPreprocessor: Boolean = true,
    val azScriptUnderlineVariables: Boolean = true,
    val azoraLangPath: String = "",
    val azoraLangDetected: Boolean = false,
    val showRuntimeWarnings: Boolean = false
)
