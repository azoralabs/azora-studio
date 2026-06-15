package org.azora.studio.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.azora.sdk.color.ColorPicker
import org.azora.sdk.color.ColorPickerMode as SdkColorPickerMode
import org.azora.sdk.color.colorToHex
import org.azora.sdk.core.project.domain.ColorPickerMode
import org.azora.sdk.core.project.domain.PaletteColor
import org.azora.sdk.core.theme.LocalAzoraPalette

/**
 * Theme settings content for managing palette colors.
 */
@Composable
fun ThemeSettingsContent(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val palette = LocalAzoraPalette.current
    val focusManager = LocalFocusManager.current
    var selectedColorId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                selectedColorId = null
            }
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Palette",
                color = palette.contentTop,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Add color button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.primary)
                    .clickable {
                        val newColor = PaletteColor(
                            name = "Color ${state.paletteColors.size + 1}",
                            hex = "#FFFFFFFF"
                        )
                        onAction(SettingsAction.AddPaletteColor(newColor))
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "+ Add Color",
                    color = palette.content,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.paletteColors.isEmpty()) {
            Text(
                text = "No colors in palette. Add colors to use them in your project.",
                color = palette.contentLow,
                fontSize = 12.sp
            )
        } else {
            // Color list
            val scrollState = rememberScrollState()

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                state.paletteColors.forEach { color ->
                    PaletteColorItem(
                        color = color,
                        isSelected = selectedColorId == color.id,
                        preferredColorPicker = state.preferredColorPicker,
                        onClick = {
                            selectedColorId = if (selectedColorId == color.id) null else color.id
                        },
                        onNameChange = { newName ->
                            onAction(SettingsAction.UpdatePaletteColor(color.copy(name = newName)))
                        },
                        onColorChange = { newHex ->
                            onAction(SettingsAction.UpdatePaletteColor(color.copy(hex = newHex)))
                        },
                        onRemove = {
                            onAction(SettingsAction.RemovePaletteColor(color.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteColorItem(
    color: PaletteColor,
    isSelected: Boolean,
    preferredColorPicker: ColorPickerMode,
    onClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        when {
            isSelected -> palette.surfaceLow
            isHovered -> palette.surfaceTop
            else -> palette.surfaceMid
        }
    )

    var nameText by remember(color.id) { mutableStateOf(color.name) }
    var hexText by remember(color.id) { mutableStateOf(color.hex) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(interactionSource)
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(hexToColor(color.hex))
                    .border(1.dp, palette.surfaceDisabled, CircleShape)
            )

            // Name and hex
            Column(modifier = Modifier.weight(1f)) {
                // Name is only editable if isEditable = true
                if (color.isEditable) {
                    BasicTextField(
                        value = nameText,
                        onValueChange = {
                            nameText = it
                            onNameChange(it)
                        },
                        textStyle = TextStyle(
                            color = palette.contentTop,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(palette.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Show name as read-only text
                    Text(
                        text = nameText,
                        color = palette.contentTop,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Hex is only editable if isEditable = true
                if (color.isEditable) {
                    BasicTextField(
                        value = hexText,
                        onValueChange = {
                            hexText = it
                            if (it.startsWith("#") && (it.length == 7 || it.length == 9)) {
                                onColorChange(it)
                            }
                        },
                        textStyle = TextStyle(
                            color = palette.contentLow,
                            fontSize = 11.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(palette.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Show hex as read-only text
                    Text(
                        text = hexText,
                        color = palette.contentLow.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            // Remove button - only show if isDeletable = true
            if (color.isDeletable && (isHovered || isSelected)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(palette.error.copy(alpha = 0.2f))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "x",
                        color = palette.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Color picker - only show when selected and editable
        AnimatedVisibility(
            visible = isSelected && color.isEditable,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                ColorPicker(
                    currentColor = hexToColor(color.hex),
                    onColorSelected = { selectedColor ->
                        val newHex = colorToHex(selectedColor)
                        hexText = newHex
                        onColorChange(newHex)
                    },
                    defaultMode = when (preferredColorPicker) {
                        ColorPickerMode.Triangle -> SdkColorPickerMode.Triangle
                        ColorPickerMode.Sliders -> SdkColorPickerMode.Sliders
                    },
                    wheelSize = 160.dp
                )
            }
        }
    }
}

/**
 * Converts a hex color string to a Compose Color.
 */
private fun hexToColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        when (cleanHex.length) {
            6 -> Color(("FF$cleanHex").toLong(16))
            8 -> Color(cleanHex.toLong(16))
            else -> Color.White
        }
    } catch (_: Exception) {
        Color.White
    }
}
