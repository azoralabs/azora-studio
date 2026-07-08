package dev.azora.studio.settings

import dev.azora.sdk.core.project.domain.*

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
    /** Per-project plugin enablement (null = legacy project, all active). */
    val projectEnabledPluginIds: List<String>? = null,
    val editorFontSize: Int = 13,
    val editorTabSize: Int = 4,
    val editorWordWrap: Boolean = false,
    val editorShowLineNumbers: Boolean = true,
    val editorAutoCloseBrackets: Boolean = true,
    val editorSmartIndent: Boolean = true,
    val editorAutoCompletion: Boolean = true,
    val editorHoverDocs: Boolean = true,
    val azScriptUsePastel: Boolean = true,
    val azScriptBoldKeywords: Boolean = true,
    val azScriptItalicPreprocessor: Boolean = true,
    val azScriptUnderlineVariables: Boolean = true,
    val azoraLangPath: String = "",
    val azoraLangDetected: Boolean = false,
    val showRuntimeWarnings: Boolean = false
)
