package dev.azora.sdk.color

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun HueWheelWithTriangle(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onHueChanged: (Float) -> Unit,
    onSaturationBrightnessChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    innerCircleColor: Color = Color(0xFF1E1E1E)
) {
    var dragTarget by remember { mutableStateOf(ColorDragTarget.None) }
    var wheelCenter by remember { mutableStateOf(Offset.Zero) }
    var wheelRadius by remember { mutableStateOf(0f) }
    var triangleSize by remember { mutableStateOf(Size.Zero) }

    // Triangle helper functions
    fun getTriangleVertices(size: Size): Triple<Offset, Offset, Offset> {
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
        return Triple(top, bottomLeft, bottomRight)
    }

    fun isInsideTriangle(pos: Offset, size: Size): Boolean {
        val (top, bottomLeft, bottomRight) = getTriangleVertices(size)

        fun sign(p1: Offset, p2: Offset, p3: Offset): Float {
            return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
        }

        val d1 = sign(pos, top, bottomLeft)
        val d2 = sign(pos, bottomLeft, bottomRight)
        val d3 = sign(pos, bottomRight, top)

        val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0)
        val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0)

        return !(hasNeg && hasPos)
    }

    fun isInsideWheelRing(pos: Offset): Boolean {
        val dx = pos.x - wheelCenter.x
        val dy = pos.y - wheelCenter.y
        val distance = sqrt(dx * dx + dy * dy)
        val innerRadius = wheelRadius * HUE_WHEEL_INNER_RATIO
        return distance >= innerRadius && distance <= wheelRadius
    }

    fun positionToSatBright(pos: Offset, size: Size): Pair<Float, Float> {
        val (top, bottomLeft, bottomRight) = getTriangleVertices(size)

        val v0 = Offset(bottomRight.x - top.x, bottomRight.y - top.y)
        val v1 = Offset(bottomLeft.x - top.x, bottomLeft.y - top.y)
        val v2 = Offset(pos.x - top.x, pos.y - top.y)

        val dot00 = v0.x * v0.x + v0.y * v0.y
        val dot01 = v0.x * v1.x + v0.y * v1.y
        val dot02 = v0.x * v2.x + v0.y * v2.y
        val dot11 = v1.x * v1.x + v1.y * v1.y
        val dot12 = v1.x * v2.x + v1.y * v2.y

        val invDenom = 1f / (dot00 * dot11 - dot01 * dot01)
        val u = (dot11 * dot02 - dot01 * dot12) * invDenom
        val v = (dot00 * dot12 - dot01 * dot02) * invDenom
        val w = 1f - u - v

        val sat = w.coerceIn(0f, 1f)
        val bright = (w + v).coerceIn(0f, 1f)

        return Pair(sat, bright)
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Convert offset to triangle coordinate space
                    val triangleOffset = Offset(
                        offset.x - (size.width - triangleSize.width) / 2,
                        offset.y - (size.height - triangleSize.height) / 2
                    )

                    when {
                        isInsideTriangle(triangleOffset, triangleSize) -> {
                            val (sat, bright) = positionToSatBright(triangleOffset, triangleSize)
                            onSaturationBrightnessChanged(sat, bright)
                        }
                        isInsideWheelRing(offset) -> {
                            val newHue = calculateHueFromPosition(offset, wheelCenter, wheelRadius, checkBounds = false)
                            if (newHue != null) onHueChanged(newHue)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val triangleOffset = Offset(
                            offset.x - (size.width - triangleSize.width) / 2,
                            offset.y - (size.height - triangleSize.height) / 2
                        )

                        dragTarget = when {
                            isInsideTriangle(triangleOffset, triangleSize) -> ColorDragTarget.Triangle
                            isInsideWheelRing(offset) -> ColorDragTarget.Wheel
                            else -> ColorDragTarget.None
                        }
                    },
                    onDragEnd = { dragTarget = ColorDragTarget.None },
                    onDragCancel = { dragTarget = ColorDragTarget.None },
                    onDrag = { change, _ ->
                        change.consume()
                        when (dragTarget) {
                            ColorDragTarget.Triangle -> {
                                val triangleOffset = Offset(
                                    change.position.x - (size.width - triangleSize.width) / 2,
                                    change.position.y - (size.height - triangleSize.height) / 2
                                )
                                val (sat, bright) = positionToSatBright(triangleOffset, triangleSize)
                                onSaturationBrightnessChanged(sat, bright)
                            }
                            ColorDragTarget.Wheel -> {
                                val newHue = calculateHueFromPosition(change.position, wheelCenter, wheelRadius, checkBounds = false)
                                if (newHue != null) onHueChanged(newHue)
                            }
                            ColorDragTarget.None -> {}
                        }
                    }
                )
            }
    ) {
        HueWheelDisplay(
            hue = hue,
            onLayout = { center, radius ->
                wheelCenter = center
                wheelRadius = radius
            },
            modifier = Modifier.fillMaxSize(),
            innerCircleColor = innerCircleColor
        )

        SaturationBrightnessTriangleDisplay(
            hue = hue,
            saturation = saturation,
            brightness = brightness,
            onLayout = { size -> triangleSize = size },
            modifier = Modifier
                .size((180 * TRIANGLE_SIZE_RATIO).dp)
                .align(Alignment.Center)
        )
    }
}
