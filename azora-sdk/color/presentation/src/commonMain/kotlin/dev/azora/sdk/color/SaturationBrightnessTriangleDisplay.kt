package dev.azora.sdk.color

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun SaturationBrightnessTriangleDisplay(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onLayout: (Size) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        onLayout(size)

        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = minOf(size.width, size.height) / 2

        val topAngle = -PI / 2
        val bottomLeftAngle = topAngle + 2 * PI / 3
        val bottomRightAngle = topAngle - 2 * PI / 3

        val top = Offset(
            centerX + radius * cos(topAngle).toFloat(),
            centerY + radius * sin(topAngle).toFloat()
        )
        val bottomLeft = Offset(
            centerX + radius * cos(bottomLeftAngle).toFloat(),
            centerY + radius * sin(bottomLeftAngle).toFloat()
        )
        val bottomRight = Offset(
            centerX + radius * cos(bottomRightAngle).toFloat(),
            centerY + radius * sin(bottomRightAngle).toFloat()
        )

        val trianglePath = Path().apply {
            moveTo(top.x, top.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            lineTo(bottomRight.x, bottomRight.y)
            close()
        }

        // Draw the triangle with color gradients
        drawPath(
            trianglePath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, Color.Black),
                startX = bottomLeft.x,
                endX = bottomRight.x
            )
        )

        drawPath(
            trianglePath,
            brush = Brush.verticalGradient(
                colors = listOf(hsvToColor(hue, 1f, 1f), Color.Transparent),
                startY = top.y,
                endY = (bottomLeft.y + bottomRight.y) / 2
            )
        )

        // Draw triangle border
        drawPath(
            trianglePath,
            color = Color.White.copy(alpha = 0.3f),
            style = Stroke(width = 1f)
        )

        // Draw indicator
        val w = saturation
        val v = (brightness - saturation).coerceIn(0f, 1f - w)
        val u = (1f - w - v).coerceIn(0f, 1f)
        val indicatorPos = Offset(
            w * top.x + v * bottomLeft.x + u * bottomRight.x,
            w * top.y + v * bottomLeft.y + u * bottomRight.y
        )

        drawCircle(
            color = Color.White,
            radius = 6f,
            center = indicatorPos,
            style = Stroke(width = 2f)
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = 4f,
            center = indicatorPos,
            style = Stroke(width = 1f)
        )
    }
}
