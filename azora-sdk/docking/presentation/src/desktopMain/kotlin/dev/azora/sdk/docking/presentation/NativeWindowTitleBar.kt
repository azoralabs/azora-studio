package dev.azora.sdk.docking.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import azora.azora_sdk.docking.presentation.generated.resources.*
import dev.azora.sdk.docking.presentation.theme.LocalDockColors
import dev.azora.sdk.docking.presentation.theme.LocalDockDimensions
import org.jetbrains.compose.resources.painterResource

/**
 * Custom title bar for native floating windows.
 *
 * Provides a custom-styled title bar that replaces the native OS window
 * decorations. The title bar is draggable (handled by wrapping in
 * [WindowDraggableArea]) and includes window control buttons.
 *
 * ## Window Controls
 *
 * From left to right:
 * 1. **Title**: The window/panel title, truncated with ellipsis if needed
 * 2. **Dock**: Returns the floating window to the main dock layout
 * 3. **Minimize**: Minimizes the window to the taskbar
 * 4. **Maximize/Restore**: Toggles between maximized and floating states
 * 5. **Close**: Closes the window and removes all contained panels
 *
 * ## Styling
 *
 * Colors and dimensions are obtained from [dev.azora.sdk.docking.presentation.theme.LocalDockColors] and
 * [dev.azora.sdk.docking.presentation.theme.LocalDockDimensions], ensuring consistency with the dock theme.
 *
 * @param title The window title to display
 * @param isMaximized Whether the window is currently maximized
 * @param isFullscreen Whether the window is currently fullscreen
 * @param onMinimize Called when minimize button is clicked
 * @param onMaximize Called when maximize/restore button is clicked
 * @param onFullscreen Called when fullscreen toggle is needed
 * @param onClose Called when close button is clicked
 * @param onDock Called when dock button is clicked
 *
 * @see NativeFloatingWindow for the parent window component
 */
@Composable
internal fun NativeWindowTitleBar(
    title: String,
    isMaximized: Boolean,
    isFullscreen: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onFullscreen: () -> Unit,
    onClose: () -> Unit,
    onDock: () -> Unit
) {
    val colors = LocalDockColors.current
    val dimensions = LocalDockDimensions.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions.panelHeaderHeight)
            .background(colors.floatingWindowHeaderBackground)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title
        Text(
            text = title,
            color = colors.panelHeaderText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Window controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dock button
            IconButton(
                onClick = onDock,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_dock_restore),
                    contentDescription = "Dock",
                    tint = colors.panelHeaderIcon,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Minimize button
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_dock_minimize),
                    contentDescription = "Minimize",
                    tint = colors.panelHeaderIcon,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Maximize/Restore button
            IconButton(
                onClick = onMaximize,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (isMaximized) Res.drawable.ic_dock_restore
                        else Res.drawable.ic_dock_maximize
                    ),
                    contentDescription = if (isMaximized) "Restore" else "Maximize",
                    tint = colors.panelHeaderIcon,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_dock_close),
                    contentDescription = "Close",
                    tint = colors.panelHeaderCloseIcon,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}