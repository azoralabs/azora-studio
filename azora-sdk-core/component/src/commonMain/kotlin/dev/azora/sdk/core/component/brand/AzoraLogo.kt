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
 * Displays the Azora logo icon.
 *
 * Renders the Azora icon without the wordmark. Use this for compact spaces
 * like app icons, favicons, or where the brand is already established.
 *
 * @param modifier Modifier to apply to the logo container.
 * @param size The size of the logo (both width and height).
 *
 * @see dev.azora.sdk.core.component.brand.AzoraBrandLogo For the full logo with wordmark.
 */
@Composable
fun AzoraLogo(
    modifier: Modifier = Modifier,
    size: Dp = 256.dp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Res.drawable.azora_logo),
            contentDescription = "Azora",
            modifier = Modifier.size(size)
        )
    }
}

@Preview
@Composable
private fun AzoraLogo_Preview() {
    AzoraLogo(
        modifier = Modifier.background(AzoraPalette.Neutral90)
    )
}