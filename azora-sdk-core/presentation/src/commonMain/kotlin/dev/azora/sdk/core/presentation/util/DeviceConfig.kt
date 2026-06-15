package dev.azora.sdk.core.presentation.util

import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import dev.azora.sdk.core.presentation.util.DeviceConfig.Companion.fromWindowSizeClass

/**
 * Enumeration representing different device configurations based on screen size and orientation.
 *
 * This enum classifies devices into five categories that help implement responsive layouts.
 * The classification is based on Material 3 window size class breakpoints:
 * - **Compact width**: < 600dp (phones in portrait)
 * - **Medium width**: 600dp - 840dp (tablets in portrait, foldables)
 * - **Expanded width**: >= 840dp (tablets in landscape, desktop)
 *
 * ## Device Categories
 *
 * | Config | Width | Height | Use Case |
 * |--------|-------|--------|----------|
 * | MOBILE_PORTRAIT | Compact | Medium+ | Phone in portrait |
 * | MOBILE_LANDSCAPE | Expanded | Compact | Phone in landscape |
 * | TABLET_PORTRAIT | Medium | Expanded | Tablet in portrait |
 * | TABLET_LANDSCAPE | Expanded | Medium | Tablet in landscape |
 * | DESKTOP | Other | Other | Desktop windows |
 *
 * ## Usage
 * ```kotlin
 * val config = currentDeviceConfig()
 *
 * // Check device type
 * if (config.isMobile) {
 *     // Show compact navigation
 * }
 *
 * // Check for wide screen layouts
 * if (config.isWideScreen) {
 *     // Show master-detail layout
 * }
 *
 * // Handle specific configurations
 * when (config) {
 *     DeviceConfig.MOBILE_PORTRAIT -> SingleColumnLayout()
 *     DeviceConfig.TABLET_LANDSCAPE -> TwoColumnLayout()
 *     // ...
 * }
 * ```
 *
 * @see currentDeviceConfig Composable to get the current device configuration
 * @see fromWindowSizeClass Factory method to create from WindowSizeClass
 */
enum class DeviceConfig {

    /** Phone in portrait orientation - compact width, medium+ height */
    MOBILE_PORTRAIT,

    /** Phone in landscape orientation - expanded width, compact height */
    MOBILE_LANDSCAPE,

    /** Tablet in portrait orientation - medium width, expanded height */
    TABLET_PORTRAIT,

    /** Tablet in landscape orientation - expanded width, medium height */
    TABLET_LANDSCAPE,

    /** Desktop window or unclassified large screen */
    DESKTOP;

    /**
     * Returns `true` if this configuration represents a mobile phone.
     *
     * Mobile devices have limited screen real estate and typically require
     * single-column layouts with full-screen navigation.
     */
    val isMobile: Boolean
        get() = this in listOf(MOBILE_PORTRAIT, MOBILE_LANDSCAPE)

    /**
     * Returns `true` if this configuration has enough width for side-by-side layouts.
     *
     * Wide screens can display master-detail layouts, permanent navigation drawers,
     * or multi-pane interfaces without feeling cramped.
     */
    val isWideScreen: Boolean
        get() = this in listOf(TABLET_LANDSCAPE, DESKTOP)

    companion object {
        /**
         * Creates a [DeviceConfig] from a [WindowSizeClass].
         *
         * This factory method analyzes the window dimensions and classifies
         * the device into the appropriate configuration category.
         *
         * @param windowSizeClass The window size class from Material 3 adaptive APIs
         * @return The corresponding [DeviceConfig] for the given window size
         */
        fun fromWindowSizeClass(windowSizeClass: WindowSizeClass) = with(windowSizeClass) {
            when {
                minWidthDp < WIDTH_DP_MEDIUM_LOWER_BOUND &&
                        minHeightDp >= HEIGHT_DP_MEDIUM_LOWER_BOUND -> MOBILE_PORTRAIT
                minWidthDp >= WIDTH_DP_EXPANDED_LOWER_BOUND &&
                        minHeightDp < HEIGHT_DP_MEDIUM_LOWER_BOUND -> MOBILE_LANDSCAPE
                minWidthDp in WIDTH_DP_MEDIUM_LOWER_BOUND..WIDTH_DP_EXPANDED_LOWER_BOUND &&
                        minHeightDp >= HEIGHT_DP_EXPANDED_LOWER_BOUND -> TABLET_PORTRAIT
                minWidthDp >= WIDTH_DP_EXPANDED_LOWER_BOUND &&
                        minHeightDp in HEIGHT_DP_MEDIUM_LOWER_BOUND..HEIGHT_DP_EXPANDED_LOWER_BOUND -> TABLET_LANDSCAPE
                else -> DESKTOP
            }
        }
    }
}