package dev.azora.sdk.core.component.textfield

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import dev.azora.sdk.core.theme.LocalAzoraPalette

/**
 * A layout wrapper for text fields that provides consistent styling and behavior.
 *
 * This internal component handles the common layout structure for text fields, including:
 * - Border styling with focus, error, and default states
 * - Background color changes based on focus and enabled states
 * - Supporting text display below the field
 * - Focus state tracking and callbacks
 * - Rounded corner styling
 * - Consistent padding and sizing
 *
 * Used by [dev.azora.sdk.core.component.textfield.AzoraTextField] and [dev.azora.sdk.core.component.textfield.AzoraPasswordTextField] to ensure visual consistency.
 *
 * @param isError Whether the field is in an error state (shows error border and text color)
 * @param supportingText Optional helper or error text displayed below the field
 * @param isEnabled Whether the field is enabled for input
 * @param onFocusChanged Callback invoked when the focus state changes
 * @param modifier Modifier to be applied to the layout container
 * @param textField Composable content that renders the actual text field with provided styling
 */
@Composable
fun AzoraTextFieldLayout(
    title: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    isEnabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    textField: @Composable (Modifier, MutableInteractionSource) -> Unit
) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember {
        MutableInteractionSource()
    }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
    }

    val textFieldStyleModifier = Modifier
        .fillMaxWidth()
        .height(36.dp)
        .then(
            when {
                isEnabled -> Modifier.dropShadow(
                    shape = RoundedCornerShape(8.dp),
                    shadow = Shadow(
                        radius = 4.dp,
                        spread = 0.dp,
                        offset = DpOffset(0.dp, 2.dp),
                        color = palette.shadow
                    )
                )
                else -> Modifier
            }
        )
        .background(
            color = when {
                isFocused -> palette.fieldFocus
                isEnabled -> palette.field
                else -> palette.surfaceDisabled
            },
            shape = RoundedCornerShape(8.dp)
        )
        .then(
            when {
                isFocused -> Modifier.border(
                    width = 1.dp,
                    shape = RoundedCornerShape(8.dp),
                    color = palette.primary
                )
                isError -> Modifier.border(
                    width = 1.dp,
                    shape = RoundedCornerShape(8.dp),
                    color = palette.error
                )
                else -> Modifier
            }
        )
        .padding(horizontal = 16.dp)

    Column(
        modifier = modifier
    ) {
        if (title != null) {
            Text(
                text = title,
                style = typography.bodySmall,
                color = palette.contentMid
            )

            Spacer(Modifier.height(8.dp))
        }

        textField(textFieldStyleModifier, interactionSource)

        if (isEnabled && supportingText != null) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = supportingText,
                style = typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = when {
                    isError -> palette.error
                    else -> palette.primary
                }
            )
        }
    }
}
