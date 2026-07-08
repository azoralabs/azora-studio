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
    @ColumnInfo(defaultValue = "13") val editorFontSize: Int = 13,
    @ColumnInfo(defaultValue = "4") val editorTabSize: Int = 4,
    @ColumnInfo(defaultValue = "0") val editorWordWrap: Boolean = false,
    @ColumnInfo(defaultValue = "1") val editorShowLineNumbers: Boolean = true,
    @ColumnInfo(defaultValue = "1") val editorAutoCloseBrackets: Boolean = true,
    @ColumnInfo(defaultValue = "1") val editorSmartIndent: Boolean = true,
    @ColumnInfo(defaultValue = "1") val editorAutoCompletion: Boolean = true,
    @ColumnInfo(defaultValue = "1") val editorHoverDocs: Boolean = true,
    val updatedAt: Long
)
