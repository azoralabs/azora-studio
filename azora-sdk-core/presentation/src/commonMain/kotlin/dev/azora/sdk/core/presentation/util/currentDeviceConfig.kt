package dev.azora.sdk.core.presentation.util

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable

/**
 * Returns the current device configuration based on window size.
 *
 * This composable function determines the device type and orientation by analyzing
 * the current window's adaptive info. Use this to implement responsive layouts
 * that adapt to different screen sizes and orientations.
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun ResponsiveLayout() {
 *     val deviceConfig = currentDeviceConfig()
 *
 *     when {
 *         deviceConfig.isMobile -> MobileLayout()
 *         deviceConfig.isWideScreen -> WideScreenLayout()
 *         else -> TabletPortraitLayout()
 *     }
 * }
 * ```
 *
 * @return The [DeviceConfig] representing the current device type and orientation
 *
 * @see DeviceConfig For available device configurations
 * @see DeviceConfig.isMobile To check if running on a mobile device
 * @see DeviceConfig.isWideScreen To check if the screen is wide enough for side-by-side layouts
 */
@Composable
fun currentDeviceConfig(): DeviceConfig {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return DeviceConfig.fromWindowSizeClass(windowSizeClass)
}