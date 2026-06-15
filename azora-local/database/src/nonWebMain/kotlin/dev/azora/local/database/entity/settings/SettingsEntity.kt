package dev.azora.local.database.entity.settings

import androidx.room.*

@Entity(tableName = "azora_settings")
data class SettingsEntity(
    @PrimaryKey
    val projectId: String,
    val editorTooltipsEnabled: Boolean,
    val tooltipDelaySeconds: Int,
    val preferredColorPicker: String,
    val paletteColorsJson: String,
    val globalConstantsJson: String,
    val azScriptUsePastel: Boolean,
    val azScriptBoldKeywords: Boolean,
    val azScriptItalicPreprocessor: Boolean,
    val azScriptUnderlineVariables: Boolean,
    val azoraLangPath: String,
    val showRuntimeWarnings: Boolean,
    val updatedAt: Long
)
