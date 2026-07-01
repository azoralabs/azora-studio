package dev.azora.sdk.core.component.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.azora.sdk.core.theme.*
import dev.azora.sdk.core.theme.font.TTRoundsNeue

@Composable
fun AzoraPreview(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = AzoraTheme.applyPreview()
    val palette = theme.palette

    val fontFamily = TTRoundsNeue
    val typography = azoraTypography(fontFamily)

    CompositionLocalProvider(
        LocalAzoraTypography provides typography,
        LocalAzoraPalette provides palette
    ) {
        MaterialTheme(
            typography = Typography(
                displayLarge = typography.displayLarge,
                displayMedium = typography.displayMedium,
                displaySmall = typography.displaySmall,
                headlineLarge = typography.headlineLarge,
                headlineMedium = typography.headlineMedium,
                headlineSmall = typography.headlineSmall,
                titleLarge = typography.titleLarge,
                titleMedium = typography.titleMedium,
                titleSmall = typography.titleSmall,
                bodyLarge = typography.bodyLarge,
                bodyMedium = typography.bodyMedium,
                bodySmall = typography.bodySmall,
                labelLarge = typography.labelLarge,
                labelMedium = typography.labelMedium,
                labelSmall = typography.labelSmall
            )
        ) {
            Box(
                modifier = modifier
                    .background(color = palette.background)
                    .padding(all = 16.dp)
            ) {
                content()
            }
        }
    }
}
