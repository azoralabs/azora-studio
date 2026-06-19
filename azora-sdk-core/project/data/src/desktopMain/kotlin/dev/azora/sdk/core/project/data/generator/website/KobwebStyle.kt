package dev.azora.sdk.core.project.data.generator.website

import dev.azora.sdk.core.project.domain.website.WebArrangement
import dev.azora.sdk.core.project.domain.website.WebFontWeight
import dev.azora.sdk.core.project.domain.website.WebModifier
import dev.azora.sdk.core.project.domain.website.WebTextAlign

/**
 * Translates the framework-agnostic [WebModifier]/style enums of the domain model into Kobweb
 * `Modifier` expression strings and layout arguments. Kept in one place so the page and navigation
 * emitters render styling identically.
 */
internal object KobwebStyle {

    /** Builds a `Modifier...` chain expression for [mod]; returns `"Modifier"` when nothing is set. */
    fun modifier(mod: WebModifier): String = buildString {
        append("Modifier")
        if (mod.fillMaxWidth) append(".fillMaxWidth()")
        if (mod.fillMaxHeight) append(".fillMaxHeight()")
        mod.width?.let { append(".width(${it}.px)") }
        mod.height?.let { append(".height(${it}.px)") }
        mod.padding?.let { append(".padding(${it}.px)") }
        colorLiteral(mod.backgroundColor)?.let { append(".backgroundColor($it)") }
        colorLiteral(mod.textColor)?.let { append(".color($it)") }
        mod.fontSize?.let { append(".fontSize(${it}.px)") }
        if (mod.fontWeight != WebFontWeight.NORMAL) append(".fontWeight(${fontWeight(mod.fontWeight)})")
        if (mod.textAlign != WebTextAlign.START) append(".textAlign(${textAlign(mod.textAlign)})")
    }

    /** Kobweb `Arrangement.*` for a column's main (vertical) axis. */
    fun columnArrangement(mod: WebModifier, arrangement: WebArrangement): String =
        mod.gap?.let { "Arrangement.spacedBy(${it}.px)" } ?: when (arrangement) {
            WebArrangement.START -> "Arrangement.Top"
            WebArrangement.CENTER -> "Arrangement.Center"
            WebArrangement.END -> "Arrangement.Bottom"
            WebArrangement.SPACE_BETWEEN -> "Arrangement.SpaceBetween"
        }

    /** Kobweb `Arrangement.*` for a row's main (horizontal) axis. */
    fun rowArrangement(mod: WebModifier, arrangement: WebArrangement): String =
        mod.gap?.let { "Arrangement.spacedBy(${it}.px)" } ?: when (arrangement) {
            WebArrangement.START -> "Arrangement.Start"
            WebArrangement.CENTER -> "Arrangement.Center"
            WebArrangement.END -> "Arrangement.End"
            WebArrangement.SPACE_BETWEEN -> "Arrangement.SpaceBetween"
        }

    /** Cross-axis alignment for a column; centers contents when the column is centered. */
    fun columnAlignment(arrangement: WebArrangement): String =
        if (arrangement == WebArrangement.CENTER) "Alignment.CenterHorizontally" else "Alignment.Start"

    /** Cross-axis alignment for a row. */
    fun rowAlignment(arrangement: WebArrangement): String =
        if (arrangement == WebArrangement.CENTER) "Alignment.CenterVertically" else "Alignment.Top"

    private fun fontWeight(weight: WebFontWeight): String = when (weight) {
        WebFontWeight.NORMAL -> "FontWeight.Normal"
        WebFontWeight.MEDIUM -> "FontWeight.Medium"
        WebFontWeight.SEMI_BOLD -> "FontWeight.SemiBold"
        WebFontWeight.BOLD -> "FontWeight.Bold"
    }

    private fun textAlign(align: WebTextAlign): String = when (align) {
        WebTextAlign.START -> "TextAlign.Start"
        WebTextAlign.CENTER -> "TextAlign.Center"
        WebTextAlign.END -> "TextAlign.End"
    }

    /** Converts a `#RRGGBB` hex string to a `Color.rgb(0x..)` literal, or `null` if unparseable. */
    fun colorLiteral(hex: String?): String? {
        if (hex.isNullOrBlank()) return null
        val cleaned = hex.removePrefix("#").trim()
        val rgb = when (cleaned.length) {
            6 -> cleaned
            3 -> cleaned.map { "$it$it" }.joinToString("")
            else -> return null
        }
        if (rgb.any { it.digitToIntOrNull(16) == null }) return null
        // Fully-qualified to avoid ambiguity with Compose-HTML's css `Color` under wildcard imports.
        return "com.varabyte.kobweb.compose.ui.graphics.Color.rgb(0x$rgb)"
    }
}
