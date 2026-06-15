@file:UseSerializers(ColorSerializer::class)

package dev.azora.sdk.core.theme.palette

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.*

/**
 * A semantic color palette for theming Azora UI components.
 *
 * This class defines colors by their purpose rather than their appearance,
 * allowing themes to map different colors to the same semantic roles.
 *
 * @property labelBackground Background color for labels and badges.
 * @property background The main background color of the application.
 * @property surfaceTop The topmost/lightest surface color for elevated components.
 * @property surfaceMid Middle-level surface color for cards and containers.
 * @property surfaceLow Lower-level surface color for nested containers.
 * @property surfaceDisabled Surface color for disabled components.
 * @property content Primary content/text color with highest contrast.
 * @property contentTop Secondary content color for less prominent text.
 * @property contentMid Tertiary content color for supporting text.
 * @property contentLow Lowest contrast content color for subtle text.
 * @property field Background color for input fields.
 * @property fieldFocus Background color for focused input fields.
 * @property placeholder Color for placeholder text in inputs.
 * @property disabled Color for disabled text and icons.
 * @property primary The primary brand color for buttons and highlights.
 * @property secondary The secondary brand color for accents.
 * @property shadow Color used for shadows and overlays.
 * @property success Color indicating successful operations.
 * @property error Color indicating errors and destructive actions.
 * @property warning Color indicating warnings and caution.
 */
@Serializable
data class Palette(
    val labelBackground: Color,
    val background: Color,
    val surfaceTop: Color,
    val surfaceMid: Color,
    val surfaceLow: Color,
    val surfaceDisabled: Color,
    val content: Color,
    val contentTop: Color,
    val contentMid: Color,
    val contentLow: Color,
    val field: Color,
    val fieldFocus: Color,
    val placeholder: Color,
    val disabled: Color,
    val primary: Color,
    val secondary: Color,
    val shadow: Color,
    val success: Color,
    val error: Color,
    val warning: Color
)