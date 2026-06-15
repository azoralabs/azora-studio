package dev.azora.sdk.core.component.dialog

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * A scrollable column layout for dialog content with consistent padding and spacing.
 *
 * Provides a standardized container for dialog body content with vertical scrolling,
 * horizontal and vertical padding, and automatic spacing between child elements.
 *
 * @param contentAlignment Horizontal alignment for child elements.
 * Defaults to [Alignment.CenterHorizontally].
 * @param content The composable content to display within the column.
 */
@Composable
fun AzoraDialogColumn(
    contentAlignment: Alignment.Horizontal? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = contentAlignment ?: Alignment.CenterHorizontally
    ) {
        content()
    }
}

@Preview
@Composable
private fun AzoraDialogColumn_Preview() =
    AzoraPreview {
        Box(Modifier.background(AzoraPalette.Neutral80)) {
            AzoraDialogColumn {
                Text(
                    text = "Dialog Title",
                    color = AzoraPalette.Neutral10
                )

                Text(
                    text = "This is the dialog content with automatic spacing.",
                    color = AzoraPalette.Neutral40
                )
            }
        }
    }