package dev.azora.sdk.core.component.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.theme.LocalAzoraPalette

/**
 * A checkbox with an animated checkmark.
 *
 * When toggled on, the box fill and border animate to the primary color and the checkmark is
 * "drawn" progressively along its two segments; toggling off reverses the animation.
 *
 * @param checked Whether the checkbox is currently checked
 * @param onCheckedChange Callback invoked with the new checked state when toggled
 * @param modifier Modifier applied to the row (box + optional label)
 * @param enabled Whether the checkbox responds to input
 * @param size The side length of the checkbox box
 * @param label Optional trailing label; the whole row is clickable when present
 */
@Composable
fun AzoraCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 20.dp,
    label: String? = null
) {
    val palette = LocalAzoraPalette.current

    val checkFraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 220)
    )
    val fill by animateColorAsState(
        targetValue = when {
            !enabled -> palette.surfaceDisabled
            checked -> palette.primary
            else -> palette.surfaceLow
        },
        animationSpec = tween(durationMillis = 180)
    )
    val stroke by animateColorAsState(
        targetValue = when {
            !enabled -> palette.disabled
            checked -> palette.primary
            else -> palette.contentLow
        },
        animationSpec = tween(durationMillis = 180)
    )
    val checkColor = if (enabled) palette.contentTop else palette.disabled

    val shape = RoundedCornerShape(size * 0.28f)

    val rowModifier = if (label != null) {
        modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange
        )
    } else {
        modifier
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val boxModifier = if (label == null) {
            Modifier.toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
        } else {
            Modifier
        }

        Box(
            modifier = boxModifier
                .size(size)
                .background(color = fill, shape = shape)
                .border(width = 1.5.dp, color = stroke, shape = shape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                // Normalized checkmark vertices scaled to the canvas.
                val p1 = Offset(0.24f * w, 0.52f * w)
                val p2 = Offset(0.43f * w, 0.70f * w)
                val p3 = Offset(0.76f * w, 0.30f * w)

                val len1 = (p2 - p1).getDistance()
                val len2 = (p3 - p2).getDistance()
                val total = len1 + len2
                val drawn = checkFraction * total
                if (drawn <= 0f) return@Canvas

                val strokeWidth = w * 0.12f

                // First segment (p1 -> p2)
                val firstEnd = if (drawn >= len1) p2 else lerpOffset(p1, p2, drawn / len1)
                drawLine(
                    color = checkColor,
                    start = p1,
                    end = firstEnd,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Second segment (p2 -> p3)
                if (drawn > len1) {
                    val secondEnd = lerpOffset(p2, p3, ((drawn - len1) / len2).coerceIn(0f, 1f))
                    drawLine(
                        color = checkColor,
                        start = p2,
                        end = secondEnd,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
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

private fun lerpOffset(start: Offset, end: Offset, fraction: Float): Offset =
    Offset(
        x = start.x + (end.x - start.x) * fraction,
        y = start.y + (end.y - start.y) * fraction
    )

@Preview(showBackground = true)
@Composable
private fun AzoraCheckbox_CheckedPreview() = AzoraPreview {
    AzoraCheckbox(
        checked = true,
        onCheckedChange = {},
        label = "Enable notifications",
        modifier = Modifier.padding(8.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun AzoraCheckbox_UncheckedPreview() = AzoraPreview {
    AzoraCheckbox(
        checked = false,
        onCheckedChange = {},
        label = "Remember me",
        modifier = Modifier.padding(8.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun AzoraCheckbox_DisabledPreview() = AzoraPreview {
    AzoraCheckbox(
        checked = true,
        onCheckedChange = {},
        enabled = false,
        label = "Locked option",
        modifier = Modifier.padding(8.dp)
    )
}
