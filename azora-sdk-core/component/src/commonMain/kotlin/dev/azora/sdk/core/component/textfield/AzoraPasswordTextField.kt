package dev.azora.sdk.core.component.textfield

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import azorastudio.azora_sdk_core.component.generated.resources.*
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.theme.LocalAzoraPalette
import org.jetbrains.compose.resources.*

/**
 * A password text field with visibility toggle functionality.
 *
 * This specialized text field provides secure password input with the ability to
 * toggle between hidden and visible text. Features include:
 * - Text obfuscation (dots/asterisks) when hidden
 * - Eye icon toggle to show/hide password
 * - All standard text field features (error states, placeholder, supporting text)
 * - Password keyboard type
 * - Ripple effect on visibility toggle icon
 *
 * @param value The current password value
 * @param onValueChange Callback invoked when the password changes
 * @param isPasswordVisible Whether the password is currently visible as plain text
 * @param onToggleVisibilityClick Callback invoked when the visibility toggle icon is clicked
 * @param modifier Modifier to be applied to the text field
 * @param placeholder Optional placeholder text shown when field is empty
 * @param supportingText Optional helper or error text shown below the field
 * @param isError Whether the field is in an error state (changes border color)
 * @param enabled Whether the field is enabled for input
 * @param onFocusChanged Callback invoked when focus state changes
 */
@Composable
fun AzoraPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onToggleVisibilityClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    isEnabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val palette = LocalAzoraPalette.current

    AzoraTextFieldLayout(
        isError = isError,
        supportingText = supportingText,
        isEnabled = isEnabled,
        onFocusChanged = onFocusChanged,
        modifier = modifier
    ) { styleModifier, interactionSource ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = styleModifier,
            enabled = isEnabled,
            singleLine = true,
            visualTransformation = when {
                isPasswordVisible -> VisualTransformation.None
                else -> PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            textStyle = typography.bodySmall.copy(
                color = when {
                    isEnabled -> palette.contentTop
                    else -> palette.disabled
                }
            ),
            interactionSource = interactionSource,
            cursorBrush = SolidColor(palette.primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                color = palette.placeholder,
                                style = typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        innerTextField()
                    }

                    Icon(
                        imageVector = when {
                            isPasswordVisible -> vectorResource(Res.drawable.eye_off_icon)
                            else -> vectorResource(Res.drawable.eye_icon)
                        },
                        contentDescription = when {
                            isPasswordVisible -> stringResource(Res.string.hide_password)
                            else -> stringResource(Res.string.show_password)
                        },
                        tint = palette.disabled,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(
                                    bounded = false,
                                    radius = 24.dp
                                ),
                                onClick = onToggleVisibilityClick
                            )
                    )
                }
            }
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun AzoraPasswordTextField_EmptyPreview() =
    AzoraPreview {
        var text by remember { mutableStateOf("") }

        AzoraPasswordTextField(
            value = text,
            onValueChange = { text = it },
            isPasswordVisible = true,
            onToggleVisibilityClick = {},
            modifier = Modifier.width(300.dp),
            placeholder = "Password",
            supportingText = "Use 9+ characters, at least one digit and one uppercase letter",
        )
    }

@Composable
@Preview(showBackground = true)
private fun AzoraPasswordTextFieldFilled_Preview() =
    AzoraPreview {
        var text by remember { mutableStateOf("password123") }

        AzoraPasswordTextField(
            value = text,
            onValueChange = { text = it },
            isPasswordVisible = false,
            onToggleVisibilityClick = {},
            modifier = Modifier.width(300.dp),
            placeholder = "Password",
            supportingText = "Use 9+ characters, at least one digit and one uppercase letter",
        )
    }

@Composable
@Preview(showBackground = true)
private fun AzoraPasswordTextField_ErrorPreview() =
    AzoraPreview {
        var text by remember { mutableStateOf("password123") }

        AzoraPasswordTextField(
            value = text,
            onValueChange = { text = it },
            isPasswordVisible = true,
            onToggleVisibilityClick = {},
            modifier = Modifier.width(300.dp),
            placeholder = "Password",
            supportingText = "Doesn't contain an uppercase character",
            isError = true,
        )
    }