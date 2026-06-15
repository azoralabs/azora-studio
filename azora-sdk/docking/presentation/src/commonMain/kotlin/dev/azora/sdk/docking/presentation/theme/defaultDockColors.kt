package dev.azora.sdk.docking.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.azora.sdk.core.theme.LocalAzoraPalette

/**
 * Creates a [DockColors] instance using the current Azora theme palette.
 *
 * This function reads colors from [LocalAzoraPalette] and maps them to
 * appropriate dock color roles. The resulting color scheme is designed
 * to integrate seamlessly with the Azora design system while providing
 * visual hierarchy and clear interactive feedback.
 *
 * ## Design Principles
 *
 * - **Panels**: Use surface colors for depth hierarchy
 * - **Tabs**: Subtle backgrounds with primary accent for active state
 * - **Splitters**: Nearly invisible until interacted with, then accent color
 * - **Drop Zones**: Primary color with transparency for clear feedback
 * - **Floating Windows**: Elevated appearance with subtle borders
 *
 * ## Custom Colors
 *
 * To customize colors, either:
 * 1. Modify the Azora palette (affects all dock colors automatically)
 * 2. Create a custom [DockColors] instance with specific overrides
 *
 * ```kotlin
 * // Option 1: Use default colors from theme
 * DockTheme(colors = defaultDockColors()) { ... }
 *
 * // Option 2: Custom colors
 * val customColors = defaultDockColors().copy(
 *     tabIndicator = Color.Red,
 *     dropZoneIndicator = Color.Green.copy(alpha = 0.2f)
 * )
 * DockTheme(colors = customColors) { ... }
 * ```
 *
 * @return A [DockColors] instance derived from the current Azora palette
 * @see DockColors
 * @see LocalAzoraPalette
 */
@Composable
fun defaultDockColors(): DockColors {
    val palette = LocalAzoraPalette.current
    return DockColors(
        // Panel - use surface colors for depth
        panelBackground = palette.surfaceTop,
        panelHeaderBackground = palette.surfaceMid,
        panelHeaderBackgroundActive = palette.surfaceMid,
        panelHeaderText = palette.contentTop.copy(alpha = 0.7f),
        panelHeaderTextActive = palette.contentTop,
        panelHeaderIcon = palette.contentTop.copy(alpha = 0.5f),
        panelHeaderCloseIcon = palette.contentTop.copy(alpha = 0.6f),
        panelBorder = palette.background,

        // Tabs - subtle with primary accent for active state
        tabBarBackground = palette.surfaceMid,
        tabBackground = Color.Transparent,
        tabBackgroundActive = palette.surfaceTop,
        tabBackgroundHover = palette.contentTop.copy(alpha = 0.05f),
        tabText = palette.contentTop.copy(alpha = 0.5f),
        tabTextActive = palette.contentTop.copy(alpha = 0.9f),
        tabIndicator = palette.primary,
        tabCloseButton = palette.contentTop.copy(alpha = 0.3f),
        tabCloseButtonHover = palette.contentTop.copy(alpha = 0.8f),
        tabCloseButtonBackground = palette.contentTop.copy(alpha = 0.1f),

        // Splitter - very subtle, accent on interaction
        splitterBackground = palette.background,
        splitterBackgroundHover = palette.primary.copy(alpha = 0.4f),
        splitterBackgroundDragging = palette.primary.copy(alpha = 0.8f),

        // Drop zones - primary color with transparency
        dropZoneIndicator = palette.primary.copy(alpha = 0.15f),
        dropZoneIndicatorBorder = palette.primary,
        dropZoneHighlight = palette.primary.copy(alpha = 0.25f),

        // Floating window - elevated appearance
        floatingWindowBackground = palette.surfaceTop,
        floatingWindowShadow = palette.shadow.copy(alpha = 0.4f),
        floatingWindowBorder = palette.primary.copy(alpha = 0.3f),
        floatingWindowHeaderBackground = palette.surfaceMid
    )
}