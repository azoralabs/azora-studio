package dev.azora.sdk.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.palette.AzoraPalette

@Composable
internal fun ColorModeSelector(
    currentMode: ColorPickerMode,
    onModeChange: (ColorPickerMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize()) {
        // Current selection
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(AzoraPalette.Neutral75)
                .clickable { expanded = !expanded }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (currentMode) {
                    ColorPickerMode.Triangle -> "Triangle"
                    ColorPickerMode.Sliders -> "Sliders"
                },
                color = AzoraPalette.Neutral45,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (expanded) "▲" else "▼",
                color = AzoraPalette.Neutral45,
                fontSize = 8.sp
            )
        }

        // Dropdown menu
        if (expanded) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .offset(y = (-54).dp)
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AzoraPalette.Neutral75)
                        .border(1.dp, AzoraPalette.Neutral65, RoundedCornerShape(4.dp))
                ) {
                    ColorPickerMode.entries.forEach { mode ->
                        Text(
                            text = when (mode) {
                                ColorPickerMode.Triangle -> "Triangle"
                                ColorPickerMode.Sliders -> "Sliders"
                            },
                            color = if (mode == currentMode) AzoraPalette.Primary else AzoraPalette.Neutral45,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable {
                                    onModeChange(mode)
                                    expanded = false
                                }
                                .pointerHoverIcon(PointerIcon.Hand)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
