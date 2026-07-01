package dev.azora.sdk.core.component.wrapper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * A container that displays content with a decorative dashed border overlay.
 *
 * This wrapper draws a rounded rectangle with a dashed stroke around its content,
 * using the primary theme color at 50% opacity. Commonly used to indicate
 * drop zones, placeholder areas, or interactive regions that accept user input.
 *
 * **Features:**
 * - Dashed border with customizable corner radius
 * - Primary color stroke at 50% opacity for subtle visibility
 * - Content is centered within the wrapper
 * - Fills available space by default
 *
 * @param modifier Optional [Modifier] for customizing the wrapper's layout and appearance.
 *
 * @param radius Corner radius for the dashed border. Defaults to 12.dp.
 * Should match the corner radius of any containing Card or surface for visual consistency.
 *
 * @param content The composable content to display inside the dashed border.
 * Content is centered both horizontally and vertically.
 *
 * @sample
 * ```
 * Card(
 *     shape = RoundedCornerShape(12.dp)
 * ) {
 *     AzoraDashWrapper {
 *         Text("Drop files here")
 *     }
 * }
 * ```
 */
@Composable
fun AzoraDashWrapper(
    modifier: Modifier = Modifier,
    radius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Dashed border overlay
        Canvas(Modifier.fillMaxSize().padding(1.dp)) {
            val stroke = Stroke(
                width = 4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(10f, 10f),
                    0f
                )
            )

            drawRoundRect(
                color = AzoraPalette.Secondary.copy(alpha = 0.5f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(radius.toPx()),
                style = stroke
            )
        }

        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun AzoraDashWrapper_Preview() = AzoraPreview {
    Card(
        modifier = Modifier
            .padding(32.dp)
            .size(128.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AzoraPalette.Neutral20
        )
    ) {
        AzoraDashWrapper {
            Text(
                text = "Content",
                color = AzoraPalette.Neutral90
            )
        }
    }
}
