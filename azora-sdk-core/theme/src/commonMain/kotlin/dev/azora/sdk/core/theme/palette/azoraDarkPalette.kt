package dev.azora.sdk.core.theme.palette

import dev.azora.sdk.core.theme.AzoraTheme

/**
 * The default dark theme palette for Azora.
 *
 * Provides a dark color scheme using colors from [AzoraPalette],
 * optimized for readability and reduced eye strain in low-light environments.
 *
 * @see azoraLightPalette for the light theme variant.
 * @see AzoraTheme.Dark for the complete dark theme.
 */
val azoraDarkPalette = Palette(
    labelBackground = AzoraPalette.Neutral95,
    background = AzoraPalette.Neutral90,
    surfaceTop = AzoraPalette.Neutral85,
    surfaceMid = AzoraPalette.Neutral80,
    surfaceLow = AzoraPalette.Neutral75,
    surfaceDisabled = AzoraPalette.Neutral65,
    content = AzoraPalette.White,
    contentTop = AzoraPalette.Neutral35,
    contentMid = AzoraPalette.Neutral40,
    contentLow = AzoraPalette.Neutral45,
    field = AzoraPalette.Neutral80,
    fieldFocus = AzoraPalette.Neutral75,
    placeholder = AzoraPalette.Neutral,
    disabled = AzoraPalette.Neutral65,
    primary = AzoraPalette.Primary,
    secondary = AzoraPalette.Secondary,
    shadow = AzoraPalette.Neutral95,
    success = AzoraPalette.AccentGreen,
    error = AzoraPalette.AccentRed,
    warning = AzoraPalette.AccentOrange
)