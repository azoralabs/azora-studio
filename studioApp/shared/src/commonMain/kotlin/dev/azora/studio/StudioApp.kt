package dev.azora.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import dev.azora.studio.editor.StudioAction
import dev.azora.studio.editor.StudioFloatingWindowsProvider
import dev.azora.studio.editor.StudioScreen
import dev.azora.studio.editor.StudioViewModel
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.theme.*
import dev.azora.sdk.core.theme.font.TTRoundsNeue
import dev.azora.sdk.core.theme.palette.*
import dev.azora.sdk.docking.domain.DockAction
import dev.azora.sdk.docking.domain.DockLayout
import dev.azora.sdk.plugin.core.InstalledPlugin
import dev.azora.sdk.plugin.presentation.PluginManager
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Studio window content.
 * Shows the main editor for working with a project.
 *
 * Note: No back navigation to Project Manager.
 * Users must close the application to return to the project browser.
 */
@Composable
fun StudioApp(
    isDarkMode: Boolean = true,
    project: AzoraProjectModel,
    projectPath: String,
    viewModel: StudioViewModel = koinViewModel { parametersOf(project, projectPath) }
) {
    val palette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
    val typography = azoraTypography(TTRoundsNeue)
    val dockState by viewModel.dockStateManager.state.collectAsState()

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
            // Provide dock theming for floating windows
            StudioFloatingWindowsProvider(
                projectPath = projectPath,
                dockState = dockState,
                onAction = viewModel::onAction
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background)
                ) {
                    StudioScreen(project = project, projectPath = projectPath, viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Data class to hold dock state for native floating windows.
 */
data class StudioDockState(
    val layout: DockLayout,
    val onAction: (DockAction) -> Unit
)

/**
 * Studio app variant that accepts an existing ViewModel.
 * Use this when the ViewModel is created at the application level for sharing.
 */
@Composable
fun StudioAppWithViewModel(
    isDarkMode: Boolean = true,
    project: AzoraProjectModel,
    projectPath: String,
    screenOffsetX: Float = 0f,
    screenOffsetY: Float = 0f,
    viewModel: StudioViewModel
) {
    val palette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
    val typography = azoraTypography(TTRoundsNeue)
    val dockState by viewModel.dockStateManager.state.collectAsState()

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
            // Provide dock theming for floating windows
            StudioFloatingWindowsProvider(
                projectPath = projectPath,
                dockState = dockState,
                onAction = viewModel::onAction
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background)
                ) {
                    StudioScreen(
                        project = project,
                        projectPath = projectPath,
                        viewModel = viewModel,
                        screenOffsetX = screenOffsetX,
                        screenOffsetY = screenOffsetY
                    )
                }
            }
        }
    }
}

/**
 * Studio app variant that exports dock state for native floating windows.
 * Use this when you need to render floating windows as real OS windows.
 */
@Composable
fun StudioAppWithStateExport(
    isDarkMode: Boolean = true,
    project: AzoraProjectModel,
    projectPath: String,
    screenOffsetX: Float = 0f,
    screenOffsetY: Float = 0f,
    onDockStateChanged: (dev.azora.sdk.docking.domain.DockState, (DockAction) -> Unit) -> Unit,
    pluginManager: PluginManager? = null,
    enabledPlugins: List<InstalledPlugin> = emptyList(),
    viewModel: StudioViewModel = koinViewModel { parametersOf(project, projectPath) }
) {
    val palette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
    val typography = azoraTypography(TTRoundsNeue)
    val dockState by viewModel.dockStateManager.state.collectAsState()

    // Create a stable callback reference
    val onDockAction = remember(viewModel) {
        { action: DockAction -> viewModel.onAction(StudioAction.DockAction(action)) }
    }

    // Export dock state to parent synchronously using SideEffect for immediate propagation
    // This is critical for drag operations where state must be available immediately
    SideEffect {
        onDockStateChanged(dockState, onDockAction)
    }

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
            // Provide dock theming for floating windows
            StudioFloatingWindowsProvider(
                projectPath = projectPath,
                dockState = dockState,
                onAction = viewModel::onAction,
                project = project,
                pluginManager = pluginManager,
                enabledPlugins = enabledPlugins
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background)
                ) {
                    StudioScreen(
                        project = project,
                        projectPath = projectPath,
                        viewModel = viewModel,
                        screenOffsetX = screenOffsetX,
                        screenOffsetY = screenOffsetY,
                        pluginManager = pluginManager,
                        enabledPlugins = enabledPlugins
                    )
                }
            }
        }
    }
}