package dev.azora.sdk.core.theme.font

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.*
import azorastudio.azora_sdk_core.theme.generated.resources.*
import org.jetbrains.compose.resources.Font

/**
 * The TT Rounds Neue Condensed font family (medium-narrow width).
 *
 * A complete font family with all weight variants (Thin to Black)
 * in both normal and italic styles. Use this for slightly condensed
 * text that needs more horizontal density than the standard variant
 * but less than the compressed variant.
 *
 * Weight mapping:
 * - W100: Thin
 * - W200: ExtraLight
 * - W300: Light
 * - W400: Regular (Normal)
 * - W500: Medium
 * - W600: DemiBold (SemiBold)
 * - W700: Bold
 * - W800: ExtraBold
 * - W900: Black
 *
 * @see TTRoundsNeue for the standard width variant.
 * @see TTRoundsNeueCompressed for the compressed (narrowest) variant.
 * @see TextStyle.condensed extension for easy conversion.
 */
val TTRoundsNeueCondensed @Composable get() = FontFamily(
    // --- Thin (100) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Thin`,
        weight = FontWeight.W100
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Thin Italic`,
        weight = FontWeight.W100,
        style = FontStyle.Italic
    ),

    // --- ExtraLight (200) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed ExtraLight`,
        weight = FontWeight.W200
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed ExtraLight Italic`,
        weight = FontWeight.W200,
        style = FontStyle.Italic
    ),

    // --- Light (300) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Light`,
        weight = FontWeight.W300
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Light Italic`,
        weight = FontWeight.W300,
        style = FontStyle.Italic
    ),

    // --- Regular (400) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Regular`,
        weight = FontWeight.W400
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Italic`,
        weight = FontWeight.W400,
        style = FontStyle.Italic
    ),

    // --- Medium (500) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Medium`,
        weight = FontWeight.W500
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Medium Italic`,
        weight = FontWeight.W500,
        style = FontStyle.Italic
    ),

    // --- DemiBold/SemiBold (600) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed DemiBold`,
        weight = FontWeight.W600
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed DemiBold Italic`,
        weight = FontWeight.W600,
        style = FontStyle.Italic
    ),

    // --- Bold (700) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Bold`,
        weight = FontWeight.W700
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Bold Italic`,
        weight = FontWeight.W700,
        style = FontStyle.Italic
    ),

    // --- ExtraBold (800) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed ExtraBold`,
        weight = FontWeight.W800
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed ExtraBold Italic`,
        weight = FontWeight.W800,
        style = FontStyle.Italic
    ),

    // --- Black (900) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Black`,
        weight = FontWeight.W900
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Condensed Black Italic`,
        weight = FontWeight.W900,
        style = FontStyle.Italic
    )
)