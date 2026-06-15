package dev.azora.sdk.core.component.divider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import dev.azora.sdk.core.component.divider.AzoraHorizontalDivider
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Displays a horizontal divider line.
 *
 * A styled horizontal line used to separate content sections.
 * Includes horizontal padding and uses the Azora neutral color palette.
 *
 * @param thick When true, renders a 1dp thick line. When false, renders a 0.5dp line.
 * @param padding Vertical padding. By default no padding.
 *
 * @see dev.azora.sdk.core.component.divider.AzoraVerticalDivider For vertical separation.
 */
@Suppress("UnusedReceiverParameter")
@Composable
fun ColumnScope.AzoraHorizontalDivider(
    thick: Boolean = false,
    padding: Dp = 0.dp
) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = padding),
        thickness = if (thick) 1.dp else 0.5.dp,
        color = AzoraPalette.Neutral70
    )
}

@Preview
@Composable
private fun AzoraHorizontalDivider_Preview() {
    Column(
        modifier = Modifier
            .width(200.dp)
            .background(AzoraPalette.Neutral90)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AzoraHorizontalDivider()

        AzoraHorizontalDivider(thick = true)
    }
}