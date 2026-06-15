package dev.azora.sdk.core.component.indicator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * A circular progress indicator with size and progress variants.
 *
 * This component wraps Material 3's CircularProgressIndicator with consistent
 * styling and sizing options. It can display either an indeterminate spinner
 * or a determinate progress arc.
 *
 * Features:
 * - Two sizes: small (24dp) and large (40dp)
 * - 4dp stroke width with rounded caps
 * - Indeterminate (animated spinner) or determinate (progress arc) modes
 * - Customizable color
 *
 * @param modifier Modifier to be applied to the indicator container
 * @param color The color of the progress indicator (default: white)
 * @param large Whether to use large size (40dp) instead of small (24dp)
 * @param progress Optional progress value (0.0 to 1.0). If null, shows indeterminate spinner
 */
@Composable
fun AzoraLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = AzoraPalette.White,
    large: Boolean = false,
    progress: Float? = null
) {
    Box(
        modifier = modifier.size(if (large) 24.dp else 16.dp)
    ) {
        if (progress != null) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color,
                strokeWidth = 1.5.dp,
                strokeCap = StrokeCap.Round
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color,
                strokeWidth = 1.5.dp,
                strokeCap = StrokeCap.Round
            )
        }
    }
}