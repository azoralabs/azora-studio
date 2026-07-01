package dev.azora.sdk.core.component.divider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Displays a vertical divider line.
 *
 * A styled vertical line used to separate content sections horizontally.
 * Includes vertical padding and uses the Azora neutral color palette.
 *
 * @param thick When true, renders a 1dp thick line. When false, renders a 0.5dp line.
 * @param padding Vertical padding. By default no padding.
 *
 * @see dev.azora.sdk.core.component.divider.AzoraHorizontalDivider For horizontal separation.
 */
@Suppress("UnusedReceiverParameter")
@Composable
fun RowScope.AzoraVerticalDivider(
    thick: Boolean = false,
    padding: Dp = 0.dp
) {
    VerticalDivider(
        modifier = Modifier.padding(vertical = padding),
        thickness = if (thick) 1.dp else 0.5.dp,
        color = AzoraPalette.Neutral70
    )
}

@Preview
@Composable
private fun AzoraVerticalDivider_Preview() = AzoraPreview {
    Row(
        modifier = Modifier
            .height(100.dp)
            .background(AzoraPalette.Neutral90)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AzoraVerticalDivider()

        AzoraVerticalDivider(thick = true)
    }
}
