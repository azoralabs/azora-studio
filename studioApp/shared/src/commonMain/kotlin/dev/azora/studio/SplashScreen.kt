package dev.azora.studio

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import dev.azora.BuildConfig
import dev.azora.sdk.core.component.brand.AzoraBrandLogo
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.theme.LocalAzoraPalette

@Composable
fun SplashScreen(
    loadingState: LoadingState,
    modifier: Modifier = Modifier
) {
    val palette = LocalAzoraPalette.current

    Box(
        modifier = modifier
            .background(palette.background)
            .border(1.dp, palette.surfaceMid)
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Logo and title
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                AzoraBrandLogo(
                    modifier = Modifier.padding(end = 4.dp),
                    width = 160.dp
                )

                Text(
                    text = "STUDIO",
                    color = palette.contentTop,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
            }

            // Bottom section - Loading info
            Column {
                // Version
                Text(
                    text = BuildConfig.STUDIO_VERSION,
                    color = palette.contentMid,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(16.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { loadingState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = palette.primary,
                    trackColor = palette.surfaceMid,
                    strokeCap = StrokeCap.Round
                )

                Spacer(Modifier.height(12.dp))

                // Loading status text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = loadingState.currentTask,
                        color = palette.contentLow,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "${(loadingState.progress * 100).toInt()}%",
                        color = palette.contentMid,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Gradient accent line at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            palette.primary,
                            palette.secondary
                        )
                    )
                )
        )
    }
}

@Preview
@Composable
private fun SplashScreen_Preview() = AzoraPreview {
    SplashScreen(
        modifier = Modifier.height(256.dp),
        loadingState = LoadingState(
            progress = 0.1f
        )
    )
}