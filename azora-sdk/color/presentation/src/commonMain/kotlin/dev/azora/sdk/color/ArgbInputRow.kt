package dev.azora.sdk.color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ArgbInputRow(
    alpha: Float,
    red: Float,
    green: Float,
    blue: Float,
    onColorChange: (Float, Float, Float, Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ArgbTextField(
            label = "A",
            value = (alpha * 255).toInt(),
            onValueChange = { onColorChange(it / 255f, red, green, blue) },
            modifier = Modifier.weight(1f)
        )

        ArgbTextField(
            label = "R",
            value = (red * 255).toInt(),
            onValueChange = { onColorChange(alpha, it / 255f, green, blue) },
            modifier = Modifier.weight(1f)
        )

        ArgbTextField(
            label = "G",
            value = (green * 255).toInt(),
            onValueChange = { onColorChange(alpha, red, it / 255f, blue) },
            modifier = Modifier.weight(1f)
        )

        ArgbTextField(
            label = "B",
            value = (blue * 255).toInt(),
            onValueChange = { onColorChange(alpha, red, green, it / 255f) },
            modifier = Modifier.weight(1f)
        )
    }
}
