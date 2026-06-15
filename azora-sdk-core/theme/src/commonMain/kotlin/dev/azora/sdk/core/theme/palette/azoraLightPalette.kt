package dev.azora.sdk.core.theme.palette

import dev.azora.sdk.core.theme.AzoraTheme

/**
 * The default light theme palette for Azora.
 *
 * Provides a light color scheme using colors from [AzoraPalette],
 * optimized for readability and visual hierarchy on light backgrounds.
 *
 * @see azoraDarkPalette for the dark theme variant.
 * @see AzoraTheme.Light for the complete light theme.
 */
val azoraLightPalette = Palette(
    labelBackground = AzoraPalette.Neutral25,
    background = AzoraPalette.Neutral10,
    surfaceTop = AzoraPalette.Neutral15,
    surfaceMid = AzoraPalette.Neutral20,
    surfaceLow = AzoraPalette.Neutral25,
    surfaceDisabled = AzoraPalette.Neutral40,
    content = AzoraPalette.Neutral95,
    contentTop = AzoraPalette.Neutral90,
    contentMid = AzoraPalette.Neutral85,
    contentLow = AzoraPalette.Neutral80,
    field = AzoraPalette.Neutral10,
    fieldFocus = AzoraPalette.White,
    placeholder = AzoraPalette.Neutral50,
    disabled = AzoraPalette.Neutral40,
    primary = AzoraPalette.Primary,
    secondary = AzoraPalette.Secondary,
    shadow = AzoraPalette.Neutral45,
    success = AzoraPalette.AccentGreen,
    error = AzoraPalette.AccentRed,
    warning = AzoraPalette.AccentOrange
)