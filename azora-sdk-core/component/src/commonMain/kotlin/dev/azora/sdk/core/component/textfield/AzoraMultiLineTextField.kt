package dev.azora.sdk.core.component.textfield

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.azora.sdk.core.component.button.AzoraButton
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * A multi-line text input field with customizable height and optional bottom content.
 *
 * This component is ideal for longer text input such as comments, descriptions, or messages.
 * Features include:
 * - Multi-line text entry with configurable maximum height
 * - Bordered container with rounded corners
 * - Optional placeholder text
 * - Optional bottom content row (e.g., action buttons)
 * - Clickable container that focuses the text field
 * - Automatic scrolling when content exceeds max height
 *
 * @param value The current text value
 * @param onValueChange Callback invoked when the text changes
 * @param modifier Modifier to be applied to the container
 * @param placeholder Optional placeholder text shown when field is empty
 * @param enabled Whether the field is enabled for input
 * @param keyboardOptions Keyboard configuration options
 * @param keyboardActions Keyboard actions configuration
 * @param maxLines Maximum number of lines (default: 5)
 * @param bottomContent Optional composable content displayed below the text field
 * (e.g., send button)
 */
@Composable
fun AzoraMultiLineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    maxLines: Int = 5,
    bottomContent: @Composable (RowScope.() -> Unit)? = null
) {
    val textFieldFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .background(
                color = AzoraPalette.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (enabled) AzoraPalette.Neutral70 else AzoraPalette.Neutral40,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = {
                    textFieldFocusRequester.requestFocus()
                }
            )
            .padding(
                vertical = 12.dp,
                horizontal = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(textFieldFocusRequester),
            textStyle = typography.bodyLarge.copy(
                color = if (enabled) AzoraPalette.Neutral70 else AzoraPalette.Neutral40,
            ),
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            cursorBrush = SolidColor(colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (placeholder != null && value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = if (enabled) AzoraPalette.Neutral60 else AzoraPalette.Neutral40,
                            style = typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (bottomContent != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomContent(this)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AzoraMultiLineTextField_Preview() = AzoraPreview {
    var text by remember {
        mutableStateOf("This is some text field content that maybe spans multiple lines")
    }

    AzoraMultiLineTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .widthIn(max = 300.dp)
            .heightIn(min = 150.dp),
        placeholder = null,
        bottomContent = {
            Spacer(Modifier.weight(1f))

            AzoraButton(
                text = "Send",
                onClick = {}
            )
        }
    )
}
