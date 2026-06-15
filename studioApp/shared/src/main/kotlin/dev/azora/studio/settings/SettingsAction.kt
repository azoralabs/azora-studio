package org.azora.studio.settings

import org.azora.sdk.core.project.domain.*

/**
 * Actions that can be performed on the settings.
 */
sealed interface SettingsAction {
    data class SetStudioTooltipsEnabled(val enabled: Boolean) : SettingsAction
    data class SetTooltipDelaySeconds(val delay: Int) : SettingsAction
    data class SetPreferredColorPicker(val mode: ColorPickerMode) : SettingsAction
    data class AddPaletteColor(val color: PaletteColor) : SettingsAction
    data class UpdatePaletteColor(val color: PaletteColor) : SettingsAction
    data class RemovePaletteColor(val colorId: String) : SettingsAction
    data class SetUseKmpRenderer(val enabled: Boolean) : SettingsAction
    data class SetAzScriptUsePastel(val usePastel: Boolean) : SettingsAction
    data class SetAzScriptBoldKeywords(val boldKeywords: Boolean) : SettingsAction
    data class SetAzScriptItalicPreprocessor(val italic: Boolean) : SettingsAction
    data class SetAzScriptUnderlineVariables(val underline: Boolean) : SettingsAction
    data class SetAzoraLangPath(val path: String) : SettingsAction
    data class SetShowRuntimeWarnings(val show: Boolean) : SettingsAction
}
