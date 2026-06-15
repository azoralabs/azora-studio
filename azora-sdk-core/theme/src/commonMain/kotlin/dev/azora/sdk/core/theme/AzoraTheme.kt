package dev.azora.sdk.core.theme

import androidx.compose.runtime.compositionLocalOf
import dev.azora.BuildConfig
import dev.azora.PreviewTheme
import dev.azora.sdk.core.theme.palette.*

/**
 * CompositionLocal providing the current [Palette] for theming.
 *
 * Defaults to [azoraDarkPalette]. Use within a CompositionLocalProvider to override.
 */
val LocalAzoraPalette = compositionLocalOf { azoraDarkPalette }

/**
 * Sealed class representing the Azora theming system.
 *
 * Provides built-in [Light] and [Dark] themes, as well as support for [Custom] themes.
 * Use the [Companion] methods to apply and query the current theme.
 *
 * @property name The display name of the theme.
 * @property palette The color palette associated with this theme.
 */
sealed class AzoraTheme(
    open val name: String,
    open val palette: Palette
) {

    /** Light theme with [azoraLightPalette]. */
    object Light : AzoraTheme("Light", azoraLightPalette)

    /** Dark theme with [azoraDarkPalette]. */
    object Dark : AzoraTheme("Dark", azoraDarkPalette)

    /**
     * A custom theme with user-defined name and palette.
     *
     * @property name The display name of the custom theme.
     * @property palette The custom color palette.
     */
    data class Custom(
        override val name: String,
        override val palette: Palette
    ) : AzoraTheme(name, palette)

    companion object {

        private lateinit var currentName: String
        private lateinit var currentPalette: Palette

        /** The name of the currently applied theme. */
        val name: String
            get() = currentName

        /** The palette of the currently applied theme. */
        val palette: Palette
            get() = currentPalette

        /**
         * Applies a theme by name and palette.
         *
         * @param name The display name for the theme.
         * @param palette The color palette to apply.
         */
        fun apply(name: String, palette: Palette) {
            currentName = name
            currentPalette = palette
        }

        /**
         * Applies the given [AzoraTheme].
         *
         * @param theme The theme to apply.
         */
        fun apply(theme: AzoraTheme) {
            currentName = theme.name
            currentPalette = theme.palette
        }

        /**
         * Applies a theme based on the build configuration's preview theme setting.
         *
         * @return The applied theme ([Light] or [Dark]).
         * @throws IllegalArgumentException If the preview theme is unknown.
         */
        fun applyPreview() = when (BuildConfig.PREVIEW_THEME) {
            PreviewTheme.LIGHT -> { apply(Light); Light }
            PreviewTheme.DARK -> { apply(Dark); Dark }
            else -> throw IllegalArgumentException(
                "Unknown preview theme: ${BuildConfig.PREVIEW_THEME}"
            )
        }

        /**
         * Returns the current theme as an [AzoraTheme] instance.
         *
         * @return [Light], [Dark], or a [Custom] theme based on the current name.
         */
        fun current() = when (currentName) {
            Light.name -> Light
            Dark.name -> Dark
            else -> Custom(currentName, currentPalette)
        }
    }
}