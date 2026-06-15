package dev.azora.sdk.color

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun HueWheelDisplay(
    hue: Float,
    onLayout: (center: Offset, radius: Float) -> Unit,
    modifier: Modifier = Modifier,
    innerCircleColor: Color = Color(0xFF1E1E1E)
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = minOf(size.width, size.height) / 2
        onLayout(center, radius)

        val outerRadius = radius
        val innerRadius = radius * HUE_WHEEL_INNER_RATIO

        // Draw color wheel segments
        for (i in 0 until 360) {
            val startAngle = i.toFloat()
            val sweepAngle = 1.5f
            val color = hsvToColor(i.toFloat(), 1f, 1f)

            drawArc(
                color = color,
                startAngle = startAngle - 90,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2)
            )
        }

        // Draw inner circle to create ring effect
        drawCircle(
            color = innerCircleColor,
            radius = innerRadius,
            center = center
        )

        // Draw hue indicator
        val indicatorAngle = (hue - 90).toDouble() * PI / 180.0
        val indicatorRadius = (outerRadius + innerRadius) / 2
        val indicatorX = center.x + indicatorRadius * cos(indicatorAngle).toFloat()
        val indicatorY = center.y + indicatorRadius * sin(indicatorAngle).toFloat()

        drawCircle(
            color = Color.White,
            radius = 6f,
            center = Offset(indicatorX, indicatorY),
            style = Stroke(width = 2f)
        )
        drawCircle(
            color = hsvToColor(hue, 1f, 1f),
            radius = 4f,
            center = Offset(indicatorX, indicatorY)
        )
    }
}
