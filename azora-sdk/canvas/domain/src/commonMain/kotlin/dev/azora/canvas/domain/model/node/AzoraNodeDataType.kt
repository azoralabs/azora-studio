package dev.azora.canvas.domain.model.node

import androidx.compose.ui.graphics.Color
import dev.azora.sdk.core.theme.palette.AzoraPalette
import kotlinx.serialization.Serializable

/**
 * Runtime type used by the script interpreter for variables, function parameters, data class
 * fields, and data port type-checking.
 *
 * Each entry carries a [label] for the UI, a [defaultValue] used when initializing a value of
 * that type without an explicit literal, and a [color] used to tint matching ports/wires so users
 * can see type compatibility at a glance.
 *
 * Distinct from [dev.azora.canvas.domain.type.AzoraPortDataType], which is the lighter-weight
 * presentation/persistence variant for canvas rendering.
 */
@Serializable
enum class AzoraNodeDataType(
    /** Human-readable name shown in pickers and headers. */
    val label: String,
    /** Default literal used when no explicit value is provided. Parsed by the interpreter. */
    val defaultValue: String,
    /** Accent color used for ports and wires of this type. */
    val color: Color
) {
    BOOLEAN("Boolean", "false", AzoraPalette.AccentRed),
    INTEGER("Integer", "0", AzoraPalette.AccentCyan),
    REAL("Real", "0.0", AzoraPalette.AccentTeal),
    STRING("String", "", AzoraPalette.AccentGreen),
    ENUM("Enum", "", AzoraPalette.AccentYellow),
    DATA_CLASS("Data Class", "", AzoraPalette.AccentOrange),
    /** Wildcard type — accepts any other data type and defers concrete typing to runtime. */
    ANY("Any", "", AzoraPalette.AccentPurple)
}