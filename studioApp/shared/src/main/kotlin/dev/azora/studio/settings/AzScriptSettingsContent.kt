package org.azora.studio.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.azora.sdk.core.theme.LocalAzoraPalette

@Composable
fun AzScriptSettingsContent(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val palette = LocalAzoraPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Azora Language",
            color = palette.contentTop,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Azora Language Path
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Installation path",
                    color = palette.contentMid,
                    fontSize = 12.sp
                )
                if (state.azoraLangDetected) {
                    Text(
                        text = "Detected",
                        color = Color(0xFF4EC9B0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (state.azoraLangPath.isNotEmpty()) {
                    Text(
                        text = "Not found",
                        color = Color(0xFFF44747),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Path to the Azora language installation (e.g. ~/.azoralang)",
                color = palette.contentLow,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            var pathText by remember(state.azoraLangPath) { mutableStateOf(state.azoraLangPath) }
            OutlinedTextField(
                value = pathText,
                onValueChange = { pathText = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = palette.contentTop,
                    fontSize = 12.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.surfaceTop,
                    cursorColor = palette.primary
                ),
                placeholder = {
                    Text(
                        text = "Auto-detected or enter path...",
                        color = palette.contentLow,
                        fontSize = 12.sp
                    )
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.primary)
                        .clickable { onAction(SettingsAction.SetAzoraLangPath(pathText)) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Apply", color = palette.content, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Text Studio",
            color = palette.contentTop,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsRow(
            label = "Syntax color palette",
            description = "Use accent or pastel colors for syntax highlighting"
        ) {
            ColorPaletteSelector(
                usePastel = state.azScriptUsePastel,
                onSelected = { onAction(SettingsAction.SetAzScriptUsePastel(it)) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsRow(
            label = "Bold keywords",
            description = "Render language keywords in bold"
        ) {
            ToggleButton(
                checked = state.azScriptBoldKeywords,
                onCheckedChange = { onAction(SettingsAction.SetAzScriptBoldKeywords(it)) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsRow(
            label = "Italic preprocessor",
            description = "Render preprocessor references in italic"
        ) {
            ToggleButton(
                checked = state.azScriptItalicPreprocessor,
                onCheckedChange = { onAction(SettingsAction.SetAzScriptItalicPreprocessor(it)) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingsRow(
            label = "Underline variables",
            description = "Render variable names with underline"
        ) {
            ToggleButton(
                checked = state.azScriptUnderlineVariables,
                onCheckedChange = { onAction(SettingsAction.SetAzScriptUnderlineVariables(it)) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Runtime",
            color = palette.contentTop,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsRow(
            label = "Show runtime warnings",
            description = "Display compiler and runtime warnings in the console output"
        ) {
            ToggleButton(
                checked = state.showRuntimeWarnings,
                onCheckedChange = { onAction(SettingsAction.SetShowRuntimeWarnings(it)) }
            )
        }
    }
}

@Composable
private fun ColorPaletteSelector(
    usePastel: Boolean,
    onSelected: (Boolean) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PaletteOption(
            label = "Pastel",
            isSelected = usePastel,
            onClick = { onSelected(true) }
        )
        PaletteOption(
            label = "Accent",
            isSelected = !usePastel,
            onClick = { onSelected(false) }
        )
    }
}

@Composable
private fun PaletteOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        when {
            isSelected -> palette.primary
            isHovered -> palette.surfaceTop
            else -> palette.surfaceLow
        }
    )

    val textColor by animateColorAsState(
        when {
            isSelected -> palette.content
            isHovered -> palette.contentTop
            else -> palette.contentMid
        }
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
