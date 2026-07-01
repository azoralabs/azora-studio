package dev.azora.sdk.core.component.textfield

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.theme.LocalAzoraPalette

/**
 * A styled text input field with support for validation, placeholders, and supporting text.
 *
 * This text field provides a consistent input experience with:
 * - Rounded corners and border styling
 * - Focus state visual feedback
 * - Error state highlighting
 * - Optional placeholder text
 * - Optional supporting/helper text below the field
 * - Single-line or multi-line support
 * - Keyboard type customization
 *
 * @param value The current text value
 * @param onValueChange Callback invoked when the text changes
 * @param modifier Modifier to be applied to the text field
 * @param placeholder Optional placeholder text shown when field is empty
 * @param title Optional title shown above the field
 * @param supportingText Error text shown below the field
 * @param isError Whether the field is in an error state (changes border color)
 * @param singleLine Whether to limit the field to a single line
 * @param isEnabled Whether the field is enabled for input
 * @param keyboardType The type of keyboard to show (e.g., Text, Email, Number)
 * @param onFocusChanged Callback invoked when focus state changes
 */
@Composable
fun AzoraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    title: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    isEnabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val palette = LocalAzoraPalette.current

    AzoraTextFieldLayout(
        title = title,
        isError = isError,
        supportingText = supportingText,
        isEnabled = isEnabled,
        onFocusChanged = onFocusChanged,
        modifier = modifier
    ) { styleModifier, interactionSource ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = isEnabled,
            singleLine = singleLine,
            textStyle = typography.bodySmall.copy(
                textDecoration = when {
                    isError -> TextDecoration.LineThrough
                    else -> null
                },
                color = when {
                    isEnabled -> palette.contentTop
                    else -> palette.disabled
                }
            ),
            cursorBrush = SolidColor(palette.primary),
            interactionSource = interactionSource,
            modifier = styleModifier,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            color = palette.placeholder,
                            style = typography.bodySmall
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun AzoraTextField_EmptyPreview() = AzoraPreview {
    var text by remember { mutableStateOf("") }

    AzoraTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.width(300.dp),
        placeholder = "Email",
        supportingText = "Please enter your email",
    )
}

@Composable
@Preview(showBackground = true)
private fun AzoraTextField_FilledPreview() = AzoraPreview {
    var text by remember { mutableStateOf("test@test.com") }

    AzoraTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.width(300.dp),
        placeholder = "Email",
        supportingText = "Please enter your email",
    )
}

@Composable
@Preview
private fun AzoraTextField_DisabledPreview() = AzoraPreview {
    var text by remember { mutableStateOf("") }

    AzoraTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.width(300.dp),
        placeholder = "Email",
        supportingText = "Please enter your email",
        isEnabled = false
    )
}

@Composable
@Preview(showBackground = true)
private fun AzoraTextField_ErrorPreview() = AzoraPreview {
    var text by remember { mutableStateOf("aaa%fg5") }

    AzoraTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.width(300.dp),
        supportingText = "This is not a valid email",
        isError = true,
    )
}
