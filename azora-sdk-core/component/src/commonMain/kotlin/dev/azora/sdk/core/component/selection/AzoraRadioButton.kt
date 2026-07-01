package dev.azora.sdk.core.component.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.theme.LocalAzoraPalette

/**
 * A single radio button whose inner dot scales in with a spring when selected.
 *
 * @param selected Whether this option is currently selected
 * @param onClick Callback invoked when the radio button is clicked
 * @param modifier Modifier applied to the row (dot + optional label)
 * @param enabled Whether the radio button responds to input
 * @param size The diameter of the outer ring
 * @param label Optional trailing label; the whole row is selectable when present
 */
@Composable
fun AzoraRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 20.dp,
    label: String? = null
) {
    val palette = LocalAzoraPalette.current

    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )
    val ring by animateColorAsState(
        targetValue = when {
            !enabled -> palette.disabled
            selected -> palette.primary
            else -> palette.contentLow
        },
        animationSpec = tween(durationMillis = 180)
    )
    val dotColor = if (enabled) palette.primary else palette.disabled

    val rowModifier = if (label != null) {
        modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick
        )
    } else {
        modifier
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val dotModifier = if (label == null) {
            Modifier.selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
        } else {
            Modifier
        }

        Canvas(modifier = dotModifier.size(size)) {
            val diameter = this.size.minDimension
            val strokeWidth = diameter * 0.1f
            // Outer ring
            drawCircle(
                color = ring,
                radius = (diameter - strokeWidth) / 2f,
                style = Stroke(width = strokeWidth)
            )
            // Inner dot, scaled by the selection animation
            if (dotScale > 0f) {
                drawCircle(
                    color = dotColor,
                    radius = (diameter * 0.28f) * dotScale
                )
            }
        }

        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) palette.contentTop else palette.disabled
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AzoraRadioButton_SelectedPreview() = AzoraPreview {
    AzoraRadioButton(
        selected = true,
        onClick = {},
        label = "Selected",
        modifier = Modifier.padding(8.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun AzoraRadioButton_UnselectedPreview() = AzoraPreview {
    AzoraRadioButton(
        selected = false,
        onClick = {},
        label = "Unselected",
        modifier = Modifier.padding(8.dp)
    )
}
