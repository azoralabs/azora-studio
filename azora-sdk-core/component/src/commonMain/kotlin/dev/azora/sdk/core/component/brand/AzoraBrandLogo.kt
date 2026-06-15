package dev.azora.sdk.core.component.brand

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import azorastudio.azora_sdk_core.component.generated.resources.*
import dev.azora.sdk.core.theme.palette.AzoraPalette
import org.jetbrains.compose.resources.painterResource

/**
 * Displays the Azora brand logo with wordmark.
 *
 * Renders the full Azora brand logo including the icon and text wordmark.
 * Use this for branding in headers, sidebars, or splash screens.
 *
 * @param modifier Modifier to apply to the logo container.
 * @param width The width of the logo. Height scales proportionally.
 *
 * @see dev.azora.sdk.core.component.brand.AzoraLogo For the icon-only version.
 */
@Composable
fun AzoraBrandLogo(
    modifier: Modifier = Modifier,
    width: Dp = 256.dp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.azora_brand_logo),
            contentDescription = "Azora",
            modifier = Modifier.width(width)
        )
    }
}

@Preview
@Composable
private fun AzoraBrandLogo_Preview() {
    AzoraBrandLogo(
        modifier = Modifier.background(AzoraPalette.Neutral90)
    )
}