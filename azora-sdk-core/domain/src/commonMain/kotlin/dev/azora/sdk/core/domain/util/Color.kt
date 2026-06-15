package dev.azora.sdk.core.domain.util

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/**
 * Converts a hex string to a [Color].
 *
 * Supports the following formats:
 * - `"0xRRGGBB"` → assumes full alpha (FF)
 * - `"0xAARRGGBB"`
 * - `"#RRGGBB"` → assumes full alpha
 * - `"#AARRGGBB"`
 *
 * Any invalid input will return [Color.Transparent].
 *
 * Examples:
 * ```
 * "0xFF0000".toColor()       // Red
 * "#80FF0000".toColor()      // Red with 50% alpha
 * "invalid".toColor()        // Transparent
 * ```
 *
 * @receiver Hex color string.
 * @return Parsed [Color] or [Color.Transparent] if invalid.
 */
fun String.toColor() = try {
    val cleanHex = this
        .removePrefix("0x")
        .removePrefix("#")

    val colorLong = cleanHex.toLong(16)

    // If only RGB is provided, force full alpha
    if (cleanHex.length == 6) {
        Color(0xFF000000 or colorLong)
    } else {
        Color(colorLong)
    }
} catch (_: Exception) {
    Color.Transparent
}

/**
 * Converts a [Color] to a hex string in ARGB format.
 *
 * Output format: `"0xAARRGGBB"`
 * Fully compatible with [String.toColor].
 *
 * Examples:
 * ```
 * Color.Red.toHexString()            // "0xFFFF0000"
 * Color(0.5f, 1f, 0f, 0f).toHexString() // "0x7FFF0000"
 * ```
 *
 * @receiver [Color] instance.
 * @return Hex string representation of the color.
 */
fun Color.toHexString(): String {
    val a = (alpha * 255).roundToInt()
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()

    return "0x" +
            a.toUInt().toString(16).padStart(2, '0').uppercase() +
            r.toUInt().toString(16).padStart(2, '0').uppercase() +
            g.toUInt().toString(16).padStart(2, '0').uppercase() +
            b.toUInt().toString(16).padStart(2, '0').uppercase()
}