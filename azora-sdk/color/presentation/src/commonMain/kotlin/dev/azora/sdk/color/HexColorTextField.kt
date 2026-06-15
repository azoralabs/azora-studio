package dev.azora.sdk.color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.palette.AzoraPalette

@Composable
internal fun HexColorTextField(
    hexColor: String,
    onHexColorChange: (String) -> Unit
) {
    var textValue by remember(hexColor) { mutableStateOf(hexColor) }
    var isEditing by remember { mutableStateOf(false) }

    BasicTextField(
        value = textValue,
        onValueChange = { newValue ->
            // Allow # and hex characters only
            val filtered = newValue.uppercase().filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }
            // Ensure it starts with # and limit to 9 chars (#AARRGGBB)
            val formatted = if (filtered.startsWith("#")) {
                filtered.take(9)
            } else {
                "#${filtered.take(8)}"
            }
            textValue = formatted

            // Only apply if valid hex (6 or 8 chars after #)
            val hexPart = formatted.removePrefix("#")
            if (hexPart.length == 6 || hexPart.length == 8) {
                onHexColorChange(formatted)
            }
        },
        textStyle = TextStyle(
            color = if (isEditing) AzoraPalette.Primary else AzoraPalette.Neutral45,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(AzoraPalette.Primary),
        singleLine = true,
        modifier = Modifier
            .background(
                if (isEditing) AzoraPalette.Neutral75 else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .onFocusChanged { focusState ->
                isEditing = focusState.isFocused
                if (!focusState.isFocused) {
                    // Reset to current color if invalid
                    textValue = hexColor
                }
            }
    )
}
