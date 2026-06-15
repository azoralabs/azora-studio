package dev.azora.sdk.color

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.*

fun parseHexColor(hex: String, fallback: Color = Color.Gray): Color = try {
    val cleanHex = hex.removePrefix("#")
    when (cleanHex.length) {
        8 -> Color(cleanHex.toLong(16)) // ARGB format
        6 -> Color(("FF$cleanHex").toLong(16)) // RGB format (assume full alpha)
        else -> fallback
    }
} catch (_: Exception) {
    fallback
}

fun argbToHex(a: Int, r: Int, g: Int, b: Int): String {
    return "#${a.coerceIn(0, 255).toHexString()}${r.coerceIn(0, 255).toHexString()}${g.coerceIn(0, 255).toHexString()}${b.coerceIn(0, 255).toHexString()}"
}

fun rgbToHex(r: Int, g: Int, b: Int): String {
    return argbToHex(255, r, g, b)
}

fun hexToColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        when (cleanHex.length) {
            8 -> { // ARGB format
                val a = cleanHex.substring(0, 2).toInt(16) / 255f
                val r = cleanHex.substring(2, 4).toInt(16) / 255f
                val g = cleanHex.substring(4, 6).toInt(16) / 255f
                val b = cleanHex.substring(6, 8).toInt(16) / 255f
                Color(r, g, b, a)
            }
            6 -> { // RGB format (assume full alpha)
                val r = cleanHex.substring(0, 2).toInt(16) / 255f
                val g = cleanHex.substring(2, 4).toInt(16) / 255f
                val b = cleanHex.substring(4, 6).toInt(16) / 255f
                Color(r, g, b)
            }
            else -> Color.White
        }
    } catch (_: Exception) {
        Color.White
    }
}

fun hsvToColor(hue: Float, saturation: Float, brightness: Float): Color {
    val c = brightness * saturation
    val x = c * (1 - abs((hue / 60) % 2 - 1))
    val m = brightness - c

    val (r, g, b) = when {
        hue < 60 -> Triple(c, x, 0f)
        hue < 120 -> Triple(x, c, 0f)
        hue < 180 -> Triple(0f, c, x)
        hue < 240 -> Triple(0f, x, c)
        hue < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f)
    )
}

fun colorToHue(color: Color): Float {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    if (delta == 0f) return 0f

    val hue = when (max) {
        r -> 60 * (((g - b) / delta) % 6)
        g -> 60 * (((b - r) / delta) + 2)
        else -> 60 * (((r - g) / delta) + 4)
    }

    return if (hue < 0) hue + 360 else hue
}

fun colorToSaturation(color: Color): Float {
    val max = maxOf(color.red, color.green, color.blue)
    val min = minOf(color.red, color.green, color.blue)

    return if (max == 0f) 0f else (max - min) / max
}

fun colorToBrightness(color: Color): Float {
    return maxOf(color.red, color.green, color.blue)
}

fun colorToHex(color: Color): String {
    val a = (color.alpha * 255).toInt().coerceIn(0, 255)
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return "#${a.toHexString()}${r.toHexString()}${g.toHexString()}${b.toHexString()}"
}

fun Int.toHexString(): String {
    val hex = this.toString(16).uppercase()
    return if (hex.length == 1) "0$hex" else hex
}

internal fun calculateHueFromPosition(
    position: Offset,
    center: Offset,
    radius: Float,
    checkBounds: Boolean = true
): Float? {
    val dx = position.x - center.x
    val dy = position.y - center.y

    if (checkBounds) {
        val distance = sqrt(dx * dx + dy * dy)
        val innerRadius = radius * HUE_WHEEL_INNER_RATIO
        if (distance < innerRadius || distance > radius) return null
    }

    var angle = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
    angle = (angle + 90 + 360) % 360
    return angle
}
