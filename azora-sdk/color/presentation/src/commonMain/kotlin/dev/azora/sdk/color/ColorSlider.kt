package dev.azora.sdk.color

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.palette.AzoraPalette

@Composable
internal fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    sliderColor: Color
) {
    var textValue by remember(value) { mutableStateOf(((value * 255).toInt()).toString()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AzoraPalette.Neutral45,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(16.dp)
        )

        // Custom slider track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AzoraPalette.Neutral75)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newValue = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                }
        ) {
            // Filled portion
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value)
                    .background(sliderColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            )
        }

        // Value text field
        BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue.filter { it.isDigit() }.take(3)
                textValue.toIntOrNull()?.coerceIn(0, 255)?.let { onValueChange(it / 255f) }
            },
            textStyle = TextStyle(
                color = AzoraPalette.Neutral35,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(AzoraPalette.Primary),
            singleLine = true,
            modifier = Modifier
                .width(40.dp)
                .background(AzoraPalette.Neutral75, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }

    // Update text when value changes externally
    LaunchedEffect(value) {
        textValue = (value * 255).toInt().toString()
    }
}
