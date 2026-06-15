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
import dev.azora.sdk.core.project.domain.ColorPickerMode
import dev.azora.sdk.core.theme.LocalAzoraPalette

/**
 * General settings content with editor and theme options.
 */
@Composable
fun GeneralSettingsContent(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val palette = LocalAzoraPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Studio section
        Text(
            text = "Studio",
            color = palette.contentTop,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Enable editor tooltips option
        SettingsRow(
            label = "Enable editor tooltips",
            description = "Show tooltips when hovering over editor elements"
        ) {
            ToggleButton(
                checked = state.editorTooltipsEnabled,
                onCheckedChange = { onAction(SettingsAction.SetStudioTooltipsEnabled(it)) }
            )
        }

        // Delay slider - only show when tooltips are enabled
        if (state.editorTooltipsEnabled) {
            Spacer(modifier = Modifier.height(12.dp))

            SettingsRow(
                label = "Tooltip delay",
                description = "Seconds to wait before showing tooltip"
            ) {
                DelaySelector(
                    selectedDelay = state.tooltipDelaySeconds,
                    onDelaySelected = { onAction(SettingsAction.SetTooltipDelaySeconds(it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Theme section
        Text(
            text = "Theme",
            color = palette.contentTop,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Preferred color picker option
        SettingsRow(
            label = "Preferred color picker",
            description = "Default color picker mode when editing colors"
        ) {
            ColorPickerModeSelector(
                selectedMode = state.preferredColorPicker,
                onModeSelected = { onAction(SettingsAction.SetPreferredColorPicker(it)) }
            )
        }
    }
}

@Composable
private fun DelaySelector(
    selectedDelay: Int,
    onDelaySelected: (Int) -> Unit
) {
    val delays = listOf(0, 1, 2, 3)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        delays.forEach { delay ->
            DelayOption(
                delay = delay,
                isSelected = selectedDelay == delay,
                onClick = { onDelaySelected(delay) }
            )
        }
    }
}

@Composable
private fun DelayOption(
    delay: Int,
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
            text = "${delay}s",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ColorPickerModeSelector(
    selectedMode: ColorPickerMode,
    onModeSelected: (ColorPickerMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorPickerMode.entries.forEach { mode ->
            ColorPickerModeOption(
                mode = mode,
                isSelected = selectedMode == mode,
                onClick = { onModeSelected(mode) }
            )
        }
    }
}

@Composable
private fun ColorPickerModeOption(
    mode: ColorPickerMode,
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
            text = mode.name,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

