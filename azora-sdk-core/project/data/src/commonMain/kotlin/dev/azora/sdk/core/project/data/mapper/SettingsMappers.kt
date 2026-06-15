package dev.azora.sdk.core.project.data.mapper

import dev.azora.local.database.entity.settings.SettingsEntity
import dev.azora.sdk.core.project.domain.*
import kotlinx.serialization.json.Json
import kotlin.time.Clock

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun SettingsModel.toEntity() = SettingsEntity(
    projectId = projectId,
    editorTooltipsEnabled = editorTooltipsEnabled,
    tooltipDelaySeconds = tooltipDelaySeconds,
    preferredColorPicker = preferredColorPicker.name,
    paletteColorsJson = json.encodeToString(paletteColors),
    globalConstantsJson = json.encodeToString(globalConstants),
    azScriptUsePastel = azScriptUsePastel,
    azScriptBoldKeywords = azScriptBoldKeywords,
    azScriptItalicPreprocessor = azScriptItalicPreprocessor,
    azScriptUnderlineVariables = azScriptUnderlineVariables,
    azoraLangPath = azoraLangPath,
    showRuntimeWarnings = showRuntimeWarnings,
    updatedAt = Clock.System.now().toEpochMilliseconds()
)

fun SettingsEntity.toModel() = SettingsModel(
    projectId = projectId,
    editorTooltipsEnabled = editorTooltipsEnabled,
    tooltipDelaySeconds = tooltipDelaySeconds,
    preferredColorPicker = ColorPickerMode.valueOf(preferredColorPicker),
    paletteColors = try {
        json.decodeFromString<List<PaletteColor>>(paletteColorsJson)
    } catch (_: Exception) {
        emptyList()
    },
    globalConstants = try {
        json.decodeFromString<List<GlobalConstant>>(globalConstantsJson)
    } catch (_: Exception) {
        emptyList()
    },
    azScriptUsePastel = azScriptUsePastel,
    azScriptBoldKeywords = azScriptBoldKeywords,
    azScriptItalicPreprocessor = azScriptItalicPreprocessor,
    azScriptUnderlineVariables = azScriptUnderlineVariables,
    azoraLangPath = azoraLangPath,
    showRuntimeWarnings = showRuntimeWarnings
)
