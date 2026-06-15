package dev.azora.sdk.core.theme.font

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Container object providing easy access to all TT Rounds Neue font family variants.
 *
 * Use this object to access alternative font widths without changing the default
 * typography configuration. All variants maintain the same weights and styles.
 *
 * Example usage:
 * ```kotlin
 * Text(
 *     text = "Compressed text",
 *     fontFamily = TTRoundsNeueFamilies.Compressed
 * )
 * ```
 *
 * @see TTRoundsNeue
 * @see TTRoundsNeueCompressed
 * @see TTRoundsNeueCondensed
 */
object TTRoundsNeueFamilies {

    /**
     * The default TT Rounds Neue font family (standard width).
     *
     * This is the primary font family used throughout the Azora design system.
     */
    val Default: FontFamily
        @Composable get() = TTRoundsNeue

    /**
     * The compressed TT Rounds Neue variant (narrowest width).
     *
     * Use for space-constrained UI elements.
     */
    val Compressed: FontFamily
        @Composable get() = TTRoundsNeueCompressed

    /**
     * The condensed TT Rounds Neue variant (medium-narrow width).
     *
     * Use for slightly condensed text with moderate horizontal density.
     */
    val Condensed: FontFamily
        @Composable get() = TTRoundsNeueCondensed
}