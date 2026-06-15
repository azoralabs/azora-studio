package dev.azora.studio.project_manager.screen.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import dev.azora.BuildConfig
import dev.azora.sdk.core.component.brand.AzoraBrandLogo
import dev.azora.sdk.core.theme.LocalAzoraPalette
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun Footer() {
    val palette = LocalAzoraPalette.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceLow)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AzoraBrandLogo(width = 80.dp)

            Text(
                text = BuildConfig.STUDIO_VERSION,
                style = typography.labelSmall,
                color = palette.contentLow
            )
        }
    }
}

@Preview
@Composable
private fun Footer_Preview() {
    Footer()
}