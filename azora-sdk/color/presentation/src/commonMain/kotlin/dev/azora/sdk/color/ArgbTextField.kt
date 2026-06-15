package dev.azora.sdk.color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.palette.AzoraPalette

@Composable
internal fun ArgbTextField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Row(modifier = modifier) {
        Text(
            text = "$label:",
            color = AzoraPalette.Neutral45,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.width(8.dp))

        BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue.filter { it.isDigit() }.take(3)
                textValue.toIntOrNull()?.coerceIn(0, 255)?.let { onValueChange(it) }
            },
            textStyle = TextStyle(
                color = AzoraPalette.Neutral35,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(AzoraPalette.Primary),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(AzoraPalette.Neutral75, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
