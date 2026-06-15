package org.azora.studio.project_manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.azora.studio.project_manager.screen.ProjectManagerScreen
import org.azora.sdk.core.theme.*
import org.azora.sdk.core.theme.font.TTRoundsNeue
import org.azora.sdk.core.theme.palette.*

/**
 * Project Manager window content.
 * Shows the project browser where users can create or open projects.
 */
@Composable
fun ProjectManagerApp(
    isDarkMode: Boolean = true,
    onProjectSelected: (projectPath: String, projectName: String) -> Unit
) {
    val palette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
    val typography = azoraTypography(TTRoundsNeue)

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
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
            ) {
                ProjectManagerScreen(
                    onProjectSelected = onProjectSelected
                )
            }
        }
    }
}