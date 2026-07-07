package dev.azora.studio.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.LocalAzoraPalette

/**
 * Settings ▸ Editor — behavior and appearance of the `.az` code editor,
 * organized IntelliJ-style into Font / Behavior / Code Assistance sections.
 * Every change persists immediately and open editors react live.
 */
@Composable
fun EditorSettingsContent(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val palette = LocalAzoraPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Editor", color = palette.contentTop, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        SectionTitle("Font & Layout")
        SettingsRow(label = "Font size", description = "Editor text size in px (Cmd/Ctrl +/− zooms temporarily)") {
            NumberStepper(value = state.editorFontSize, min = 9, max = 28) {
                onAction(SettingsAction.SetEditorFontSize(it))
            }
        }
        SettingsRow(label = "Tab size", description = "Spaces inserted per indent level") {
            NumberStepper(value = state.editorTabSize, min = 2, max = 8) {
                onAction(SettingsAction.SetEditorTabSize(it))
            }
        }
        SettingsRow(label = "Word wrap", description = "Wrap long lines instead of scrolling horizontally") {
            ToggleButton(state.editorWordWrap) { onAction(SettingsAction.SetEditorWordWrap(it)) }
        }
        SettingsRow(label = "Line numbers", description = "Show the gutter with line numbers and breakpoints") {
            ToggleButton(state.editorShowLineNumbers) { onAction(SettingsAction.SetEditorLineNumbers(it)) }
        }

        SectionTitle("Typing Behavior")
        SettingsRow(label = "Auto-close brackets", description = "Insert the matching ), ], } or \" and type over closers") {
            ToggleButton(state.editorAutoCloseBrackets) { onAction(SettingsAction.SetEditorAutoCloseBrackets(it)) }
        }
        SettingsRow(label = "Smart indent", description = "Keep indentation on Enter and indent after {") {
            ToggleButton(state.editorSmartIndent) { onAction(SettingsAction.SetEditorSmartIndent(it)) }
        }

        SectionTitle("Code Assistance")
        SettingsRow(label = "Completion while typing", description = "Open suggestions automatically (Ctrl/Cmd+Space always works)") {
            ToggleButton(state.editorAutoCompletion) { onAction(SettingsAction.SetEditorAutoCompletion(it)) }
        }
        SettingsRow(label = "Hover documentation", description = "Show signatures when resting the pointer on a symbol") {
            ToggleButton(state.editorHoverDocs) { onAction(SettingsAction.SetEditorHoverDocs(it)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val palette = LocalAzoraPalette.current
    Text(
        text = text,
        color = palette.contentMid,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/** Small − N + stepper for numeric preferences. */
@Composable
private fun NumberStepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val palette = LocalAzoraPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StepperButton("−", enabled = value > min) { onChange(value - 1) }
        Text(
            text = value.toString(),
            color = palette.contentTop,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(28.dp),
        )
        StepperButton("+", enabled = value < max) { onChange(value + 1) }
    }
}

@Composable
private fun StepperButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalAzoraPalette.current
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (enabled) palette.surfaceTop else palette.surfaceLow)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) palette.contentTop else palette.contentLow, fontSize = 13.sp)
    }
}
