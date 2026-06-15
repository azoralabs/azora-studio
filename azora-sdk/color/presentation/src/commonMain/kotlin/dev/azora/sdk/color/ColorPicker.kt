package dev.azora.sdk.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * A color picker component with triangle and slider modes.
 *
 * @param currentColor The currently selected color.
 * @param onColorSelected Callback when a color is selected.
 * @param modifier Modifier for the component.
 * @param defaultMode The default color picker mode.
 * @param wheelSize Size of the color wheel.
 */
@Composable
fun ColorPicker(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    defaultMode: ColorPickerMode = ColorPickerMode.Triangle,
    wheelSize: Dp = 180.dp
) {
    var mode by remember { mutableStateOf(defaultMode) }

    // Store RGB values directly - initialized once from currentColor
    var alpha by remember { mutableStateOf(currentColor.alpha) }
    var red by remember { mutableStateOf(currentColor.red) }
    var green by remember { mutableStateOf(currentColor.green) }
    var blue by remember { mutableStateOf(currentColor.blue) }

    // HSV values for triangle mode - derived from RGB
    var hue by remember { mutableStateOf(colorToHue(currentColor)) }
    var saturation by remember { mutableStateOf(colorToSaturation(currentColor)) }
    var brightness by remember { mutableStateOf(colorToBrightness(currentColor)) }

    val currentRgbColor = Color(red, green, blue, alpha)

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (mode) {
            ColorPickerMode.Triangle -> {
                // Color wheel for hue selection
                HueWheelWithTriangle(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onHueChanged = { newHue ->
                        hue = newHue
                        val newColor = hsvToColor(newHue, saturation, brightness)
                        red = newColor.red
                        green = newColor.green
                        blue = newColor.blue
                        onColorSelected(newColor.copy(alpha = alpha))
                    },
                    onSaturationBrightnessChanged = { newSat, newBright ->
                        saturation = newSat
                        brightness = newBright
                        val newColor = hsvToColor(hue, newSat, newBright)
                        red = newColor.red
                        green = newColor.green
                        blue = newColor.blue
                        onColorSelected(newColor.copy(alpha = alpha))
                    },
                    modifier = Modifier
                        .size(wheelSize)
                        .align(Alignment.CenterHorizontally),
                    innerCircleColor = AzoraPalette.Neutral80
                )
            }

            ColorPickerMode.Sliders -> {
                // ARGB Sliders
                ColorSlider(
                    label = "A",
                    value = alpha,
                    onValueChange = { newAlpha ->
                        alpha = newAlpha
                        onColorSelected(Color(red, green, blue, newAlpha))
                    },
                    sliderColor = Color.Gray
                )
                ColorSlider(
                    label = "R",
                    value = red,
                    onValueChange = { newRed ->
                        red = newRed
                        val c = Color(newRed, green, blue)
                        hue = colorToHue(c)
                        saturation = colorToSaturation(c)
                        brightness = colorToBrightness(c)
                        onColorSelected(c.copy(alpha = alpha))
                    },
                    sliderColor = Color.Red
                )
                ColorSlider(
                    label = "G",
                    value = green,
                    onValueChange = { newGreen ->
                        green = newGreen
                        val c = Color(red, newGreen, blue)
                        hue = colorToHue(c)
                        saturation = colorToSaturation(c)
                        brightness = colorToBrightness(c)
                        onColorSelected(c.copy(alpha = alpha))
                    },
                    sliderColor = Color.Green
                )
                ColorSlider(
                    label = "B",
                    value = blue,
                    onValueChange = { newBlue ->
                        blue = newBlue
                        val c = Color(red, green, newBlue)
                        hue = colorToHue(c)
                        saturation = colorToSaturation(c)
                        brightness = colorToBrightness(c)
                        onColorSelected(c.copy(alpha = alpha))
                    },
                    sliderColor = Color.Blue
                )
            }
        }

        if (mode == ColorPickerMode.Triangle) {
            Spacer(Modifier.height(4.dp))

            // ARGB text fields row
            ArgbInputRow(
                alpha = alpha,
                red = red,
                green = green,
                blue = blue,
                onColorChange = { a, r, g, b ->
                    alpha = a
                    red = r
                    green = g
                    blue = b
                    // Sync to HSV for triangle mode
                    val newColor = Color(r, g, b)
                    hue = colorToHue(newColor)
                    saturation = colorToSaturation(newColor)
                    brightness = colorToBrightness(newColor)
                    onColorSelected(Color(r, g, b, a))
                }
            )
        }

        Spacer(Modifier.height(4.dp))

        // Color preview, hex display, and mode selector
        val clipboardManager = LocalClipboardManager.current
        val hexColor = colorToHex(currentRgbColor)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color preview - click to copy hex to clipboard
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, AzoraPalette.Neutral65, RoundedCornerShape(10.dp))
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentRgbColor)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { clipboardManager.setText(AnnotatedString(hexColor)) }
                )

                // Hex value (editable)
                HexColorTextField(
                    hexColor = hexColor,
                    onHexColorChange = { newHex ->
                        val newColor = hexToColor(newHex)
                        alpha = newColor.alpha
                        red = newColor.red
                        green = newColor.green
                        blue = newColor.blue
                        hue = colorToHue(newColor)
                        saturation = colorToSaturation(newColor)
                        brightness = colorToBrightness(newColor)
                        onColorSelected(newColor)
                    }
                )
            }

            // Mode selector dropdown
            ColorModeSelector(
                currentMode = mode,
                onModeChange = { mode = it }
            )
        }
    }
}
