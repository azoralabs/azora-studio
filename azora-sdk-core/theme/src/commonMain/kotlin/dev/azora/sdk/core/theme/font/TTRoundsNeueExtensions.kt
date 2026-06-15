package dev.azora.sdk.core.theme.font

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

/**
 * Creates a copy of this [TextStyle] using the [TTRoundsNeueCompressed] font family.
 *
 * This extension allows easy conversion of any text style to use the compressed
 * font variant while preserving all other style properties.
 *
 * Example usage:
 * ```kotlin
 * Text(
 *     text = "Compressed headline",
 *     style = typography.headlineLarge.compressed
 * )
 * ```
 *
 * @return A new [TextStyle] with [TTRoundsNeueFamilies.Compressed] as the font family.
 */
val TextStyle.compressed: TextStyle
    @Composable
    get() = this.copy(fontFamily = TTRoundsNeueFamilies.Compressed)

/**
 * Creates a copy of this [TextStyle] using the [TTRoundsNeueCondensed] font family.
 *
 * This extension allows easy conversion of any text style to use the condensed
 * font variant while preserving all other style properties.
 *
 * Example usage:
 * ```kotlin
 * Text(
 *     text = "Condensed body text",
 *     style = typography.bodyMedium.condensed
 * )
 * ```
 *
 * @return A new [TextStyle] with [TTRoundsNeueFamilies.Condensed] as the font family.
 */
val TextStyle.condensed: TextStyle
    @Composable
    get() = this.copy(fontFamily = TTRoundsNeueFamilies.Condensed)