package dev.azora.sdk.core.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import dev.azora.sdk.core.component.button.AzoraButton
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.theme.LocalAzoraPalette
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * A reusable Azora dialog with a centered card layout.
 *
 * This dialog provides a consistent, elevated card-based UI for displaying modal content.
 * The dialog's behavior and appearance can be customized through its parameters.
 *
 * **Features:**
 * - Displays content inside a rounded, elevated card (400.dp width, 30.dp corner radius)
 * - Content is scrollable via a vertical scroll container
 * - Customizable dismiss behavior via [DialogProperties]
 * - Flexible content alignment options
 * - Built-in spacing and padding for consistent layout
 *
 * **Default Behavior:**
 * - **NOT** dismissible by pressing back or clicking outside (modal dialog)
 * - Does not use platform default width (full control over positioning)
 * - Centers content horizontally within the card
 *
 * **Note:** To create a dismissible dialog, provide custom [DialogProperties] with
 * `dismissOnBackPress = true` and/or `dismissOnClickOutside = true`.
 *
 * @param onDismissRequest Callback invoked when the user attempts to dismiss the dialog.
 * By default, the dialog is non-dismissible, so this is only triggered if custom
 * [properties] enable dismiss behavior, or if the dialog is programmatically dismissed.
 *
 * @param contentAlignment Optional horizontal alignment of the content inside the card.
 * If `null`, defaults to [Alignment.CenterHorizontally].
 *
 * @param properties Optional [DialogProperties] to customize dialog behavior
 * (dismiss on back press, dismiss on outside click, platform width usage).
 * If `null`, uses default properties that prevent dismissal (modal behavior).
 *
 * @param content The UI content to display inside the dialog. This is
 * provided as a [ColumnScope] lambda so that column-scoped modifiers
 * (e.g., `weight`, `align`) can be used. Content is automatically spaced
 * with 16.dp vertical arrangement and includes 20.dp horizontal / 10.dp vertical padding.
 *
 * @sample
 * ```
 * // Non-dismissible modal dialog (default)
 * AzoraDialog(
 *     onDismissRequest = { /* Only called on programmatic dismiss */ }
 * ) {
 *     Text("Dialog Title", style = MaterialTheme.typography.headlineSmall)
 *     Text("Dialog content goes here")
 *     Button(onClick = { showDialog = false }) {
 *         Text("Close")
 *     }
 * }
 *
 * // Dismissible dialog (custom properties)
 * AzoraDialog(
 *     onDismissRequest = { showDialog = false },
 *     properties = DialogProperties(
 *         dismissOnBackPress = true,
 *         dismissOnClickOutside = true
 *     )
 * ) {
 *     Text("Dismissible Dialog")
 * }
 * ```
 */
@Composable
fun AzoraDialog(
    onDismissRequest: () -> Unit = {},
    contentAlignment: Alignment.Horizontal? = null,
    properties: DialogProperties? = null,
    width: Dp = 400.dp,
    bottom: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalAzoraPalette.current

    Dialog(
        onDismissRequest,
        properties ?: DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.width(width),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = palette.background
                )
            ) {
                Column {
                    AzoraDialogColumn(
                        contentAlignment
                    ) {
                        content()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.labelBackground)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        bottom()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AzoraDialog_Preview() =
    AzoraPreview {
        val palette = LocalAzoraPalette.current

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.width(400.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = palette.background
                )
            ) {
                AzoraDialogColumn {
                    Text(
                        text = "Dialog Title",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.contentTop
                    )

                    Text(
                        text = "This is the dialog content.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.contentMid
                    )

                    Spacer(Modifier)

                    AzoraButton(
                        text = "Confirm",
                        {})
                }
            }
        }
    }