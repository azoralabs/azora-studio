package dev.azora.sdk.core.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.component.indicator.AzoraLoadingIndicator
import dev.azora.sdk.core.theme.LocalAzoraPalette
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Defines the visual style variants for [dev.azora.sdk.core.component.button.AzoraButton].
 */
enum class AzoraButtonStyle {

    /**
     * Primary button style with filled background using the primary brand color.
     * Use for main call-to-action buttons.
     */
    PRIMARY,

    /**
     * Secondary button style with white background and border.
     * Use for alternative actions or less prominent buttons.
     */
    SECONDARY
}

/**
 * A versatile button component with support for primary and secondary styles,
 * loading states, and optional leading icons.
 *
 * This button automatically handles:
 * - Primary and secondary visual styles
 * - Enabled/disabled states with appropriate color changes
 * - Loading state with a spinner indicator
 * - Optional leading icon before text
 * - Rounded corners and proper padding
 *
 * @param text The text label displayed on the button
 * @param onClick Callback invoked when the button is clicked (ignored when [processing] is true)
 * @param modifier Modifier to be applied to the button
 * @param style Visual style of the button (PRIMARY or SECONDARY)
 * @param enabled Whether the button is enabled for interaction
 * @param textStyle Typography style for the button text
 * @param fontWeight Font weight for the button text
 * @param processing When true, shows a loading indicator and ignores clicks
 * @param leadingIcon Optional composable icon displayed before the text
 */
@Composable
fun AzoraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AzoraButtonStyle = AzoraButtonStyle.PRIMARY,
    enabled: Boolean = true,
    textStyle: TextStyle = typography.labelSmall,
    fontWeight: FontWeight = FontWeight.Medium,
    processing: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val palette = LocalAzoraPalette.current

    val colors = when (style) {
        AzoraButtonStyle.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = palette.primary,
            contentColor = palette.content,
            disabledContainerColor = AzoraPalette.Transparent,
            disabledContentColor = palette.disabled
        )
        AzoraButtonStyle.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = palette.secondary,
            contentColor = palette.content,
            disabledContainerColor = AzoraPalette.Transparent,
            disabledContentColor = palette.disabled
        )
    }

    Button(
        onClick = { if (!processing) onClick() },
        modifier = modifier
            .dropShadow(
                shape = RoundedCornerShape(8.dp),
                shadow = Shadow(
                    radius = 4.dp,
                    spread = 0.dp,
                    offset = DpOffset(0.dp, 2.dp),
                    color = palette.shadow
                )
            )
            .height(36.dp),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = colors,
        border = BorderStroke(
            width = 1.dp,
            color = when (style) {
                AzoraButtonStyle.SECONDARY -> palette.contentTop
                else -> AzoraPalette.Transparent
            }
        )
    ) {
        when {
            processing -> AzoraLoadingIndicator()
            else -> Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.invoke()

                Text(
                    text = text,
                    style = textStyle,
                    fontWeight = fontWeight,
                    overflow = TextOverflow.Ellipsis,
                    color = AzoraPalette.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AzoraButton_PrimaryPreview() =
    AzoraPreview {
        AzoraButton(
            text = "Primary",
            onClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }

@Preview(showBackground = true)
@Composable
private fun AzoraButton_PrimaryProcessingPreview() =
    AzoraPreview {
        AzoraButton(
            text = "Primary",
            onClick = {},
            processing = true,
            modifier = Modifier.padding(8.dp)
        )
    }

@Preview(showBackground = true)
@Composable
private fun AzoraButton_SecondaryPreview() =
    AzoraPreview {
        AzoraButton(
            text = "Secondary",
            onClick = {},
            style = AzoraButtonStyle.SECONDARY,
            modifier = Modifier.padding(8.dp)
        )
    }

@Preview(showBackground = true)
@Composable
private fun AzoraButton_SecondaryProcessingPreview() =
    AzoraPreview {
        AzoraButton(
            text = "Primary",
            onClick = {},
            style = AzoraButtonStyle.SECONDARY,
            processing = true,
            modifier = Modifier.padding(8.dp)
        )
    }