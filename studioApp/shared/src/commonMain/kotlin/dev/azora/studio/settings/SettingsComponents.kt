package dev.azora.studio.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.LocalAzoraPalette

@Composable
internal fun SettingsRow(
    label: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalAzoraPalette.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = palette.contentTop,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = palette.contentLow,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        content()
    }
}

@Composable
internal fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        when {
            checked -> palette.primary
            isHovered -> palette.surfaceTop
            else -> palette.surfaceLow
        }
    )

    val indicatorColor by animateColorAsState(
        if (checked) palette.content else palette.contentLow
    )

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(indicatorColor)
        )
    }
}
