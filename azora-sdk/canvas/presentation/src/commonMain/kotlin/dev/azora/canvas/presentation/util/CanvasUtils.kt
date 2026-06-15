package dev.azora.canvas.presentation.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import dev.azora.canvas.domain.type.AzoraPortDataType
import dev.azora.canvas.domain.type.AzoraPortType
import dev.azora.sdk.core.theme.palette.AzoraPalette
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Theme color used to render execution-flow links and ports of this type.
 *
 * For [AzoraPortType.NAV_PUSH_REPLACE] (the split-shape port) this returns the default green of
 * the push half — the orange replace half is drawn separately by the port composable.
 */
fun AzoraPortType.toColor(): Color = when (this) {
    AzoraPortType.NAV_ROOT -> AzoraPalette.AccentRed
    AzoraPortType.NAV_PUSH -> AzoraPalette.AccentGreen
    AzoraPortType.NAV_REPLACE -> AzoraPalette.AccentOrange
    AzoraPortType.NAV_PUSH_REPLACE -> AzoraPalette.AccentGreen
    AzoraPortType.NAV_DIALOG -> AzoraPalette.White
}

/**
 * Theme color used to render data-flow ports of this type. Hex literals are used directly here
 * (rather than [AzoraPalette]) because the data-port palette is intentionally separate from the
 * navigation palette so users can tell exec ports and data ports apart at a glance.
 */
fun AzoraPortDataType.toColor(): Color = when (this) {
    AzoraPortDataType.BOOL -> AzoraPalette.AccentRed
    AzoraPortDataType.INTEGER -> Color(0xFF33CCCC)
    AzoraPortDataType.REAL -> Color(0xFF33CC33)
    AzoraPortDataType.TEXT -> Color(0xFFCC33CC)
    AzoraPortDataType.ENUM -> Color(0xFFCCCC33)
    AzoraPortDataType.DATA_CLASS -> Color(0xFF3366CC)
    AzoraPortDataType.ANY -> Color(0xFF999999)
}

/**
 * Tests whether [point] lies within [threshold] pixels of the cubic Bézier defined by [p0]..[p3].
 *
 * Used by [dev.azora.canvas.presentation.canvas.AzoraLinksLayer] for link hover hit-testing. The
 * curve is sampled at 100 fixed `t` values and the test returns true on the first sample inside
 * the threshold; this is cheap and good enough for hover at typical link curvatures, but is not a
 * true geometric distance — narrow misses very close to inflection points may slip through.
 *
 * @param point The point to test, in the same coordinate space as the control points.
 * @param p0 Curve start.
 * @param p1 First control point.
 * @param p2 Second control point.
 * @param p3 Curve end.
 * @param threshold Distance in pixels under which the point is considered "near" the curve.
 */
fun isPointNearBezier(
    point: Offset,
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    threshold: Float
): Boolean {
    val samples = 100
    for (i in 0..samples) {
        val t = i.toFloat() / samples
        val bezierPoint = cubicBezierPoint(t, p0, p1, p2, p3)
        val distance = sqrt(
            (point.x - bezierPoint.x).pow(2) + (point.y - bezierPoint.y).pow(2)
        )
        if (distance <= threshold) {
            return true
        }
    }
    return false
}

/**
 * Evaluate the cubic Bézier B(t) defined by [p0]..[p3] at parameter [t] ∈ [0, 1].
 *
 * Implements `(1-t)³·p0 + 3(1-t)²t·p1 + 3(1-t)t²·p2 + t³·p3` directly to avoid creating
 * intermediate `Offset`s.
 */
fun cubicBezierPoint(t: Float, p0: Offset, p1: Offset, p2: Offset, p3: Offset): Offset {
    val u = 1 - t
    val tt = t * t
    val uu = u * u
    val uuu = uu * u
    val ttt = tt * t

    val x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x
    val y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y
    return Offset(x, y)
}

/**
 * Lighten a color by mixing it toward white by [factor].
 *
 * Linearly interpolates each RGB channel toward 1.0 (factor 0 leaves the color unchanged, factor 1
 * yields pure white). Alpha is preserved. Used for hover and selection emphasis on links and ports.
 *
 * @param factor Brighten amount in the closed range `[0.0, 1.0]`. Values outside the range are
 *   tolerated thanks to channel clamping but are not meaningful.
 */
fun Color.brighten(factor: Float): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceIn(0f, 1f),
        green = (green + (1f - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

/**
 * Build a right-pointing rounded triangle path for execution-flow ports.
 *
 * The triangle is inscribed in a [width] × [height] rectangle anchored at ([offsetX], [offsetY]),
 * with [cornerRadius] applied to its three corners (top-left, bottom-left, and the right tip).
 * Used by [dev.azora.canvas.presentation.node.AzoraInputPort] and
 * [dev.azora.canvas.presentation.node.AzoraOutputPort] to draw both the outer port shape and the
 * inner "unconnected" indicator.
 *
 * @param width Triangle bounding-box width.
 * @param height Triangle bounding-box height.
 * @param cornerRadius Corner radius applied to all three corners.
 * @param offsetX X offset of the triangle's bounding box.
 * @param offsetY Y offset of the triangle's bounding box.
 */
fun createRoundedTrianglePath(
    width: Float,
    height: Float,
    cornerRadius: Float,
    offsetX: Float = 0f,
    offsetY: Float = 0f
): Path = Path().apply {
    val r = cornerRadius
    // Start at top-left, after the rounded corner
    moveTo(offsetX + r, offsetY)
    // Line to right tip (before curve)
    lineTo(offsetX + width - r, offsetY + height / 2 - r)
    // Rounded right tip
    quadraticTo(offsetX + width, offsetY + height / 2, offsetX + width - r, offsetY + height / 2 + r)
    // Line to bottom-left (before curve)
    lineTo(offsetX + r, offsetY + height)
    // Rounded bottom-left corner
    quadraticTo(offsetX, offsetY + height, offsetX, offsetY + height - r)
    // Line up to top-left (before curve)
    lineTo(offsetX, offsetY + r)
    // Rounded top-left corner
    quadraticTo(offsetX, offsetY, offsetX + r, offsetY)
    close()
}