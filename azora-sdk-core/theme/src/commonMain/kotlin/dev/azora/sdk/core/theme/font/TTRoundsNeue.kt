package dev.azora.sdk.core.theme.font

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.*
import azorastudio.azora_sdk_core.theme.generated.resources.*
import org.jetbrains.compose.resources.Font

/**
 * The TT Rounds Neue font family (standard width).
 *
 * A complete font family with all weight variants (Thin to Black)
 * in both normal and italic styles. This is the default font family
 * used in the Azora design system.
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
 * @see TTRoundsNeueCompressed for the compressed width variant.
 * @see TTRoundsNeueCondensed for the condensed width variant.
 */
val TTRoundsNeue @Composable get() = FontFamily(
    // --- Thin (100) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Thin`,
        weight = FontWeight.W100
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Thin Italic`,
        weight = FontWeight.W100,
        style = FontStyle.Italic
    ),

    // --- ExtraLight (200) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial ExtraLight`,
        weight = FontWeight.W200
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial ExtraLight Italic`,
        weight = FontWeight.W200,
        style = FontStyle.Italic
    ),

    // --- Light (300) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Light`,
        weight = FontWeight.W300
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Light Italic`,
        weight = FontWeight.W300,
        style = FontStyle.Italic
    ),

    // --- Regular (400) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Regular`,
        weight = FontWeight.W400 // == FontWeight.Normal
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Italic`,
        weight = FontWeight.W400,
        style = FontStyle.Italic
    ),

    // --- Medium (500) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Medium`,
        weight = FontWeight.W500 // == FontWeight.Medium
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Medium Italic`,
        weight = FontWeight.W500,
        style = FontStyle.Italic
    ),

    // --- DemiBold/SemiBold (600) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial DemiBold`,
        weight = FontWeight.W600 // == FontWeight.SemiBold
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial DemiBold Italic`,
        weight = FontWeight.W600,
        style = FontStyle.Italic
    ),

    // --- Bold (700) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Bold`,
        weight = FontWeight.W700 // == FontWeight.Bold
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Bold Italic`,
        weight = FontWeight.W700,
        style = FontStyle.Italic
    ),

    // --- ExtraBold (800) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial ExtraBold`,
        weight = FontWeight.W800 // == FontWeight.ExtraBold
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial ExtraBold Italic`,
        weight = FontWeight.W800,
        style = FontStyle.Italic
    ),

    // --- Black (900) ---
    Font(
        resource = Res.font.`TT Rounds Neue Trial Black`,
        weight = FontWeight.W900 // == FontWeight.Black
    ),
    Font(
        resource = Res.font.`TT Rounds Neue Trial Black Italic`,
        weight = FontWeight.W900,
        style = FontStyle.Italic
    ),
)