package dev.azora.sdk.core.presentation.lifecycle

import androidx.compose.ui.window.*
import java.awt.Frame

/**
 * Desktop (JVM) implementation of [AppMinimizer] using Compose Window APIs and AWT.
 *
 * This implementation supports two methods of window minimization:
 * 1. **Compose WindowState**: Sets [WindowState.isMinimized] to true
 * 2. **AWT Frame**: Sets frame state to [Frame.ICONIFIED]
 *
 * Both methods are applied when [minimize] is called to ensure compatibility
 * with different desktop window configurations.
 *
 * ## Window State Management
 * The minimizer stores references to:
 * - [WindowState] from Compose Desktop's window management
 * - [Frame] from AWT for direct window control
 *
 * ## Behavior
 * - Window minimizes to the taskbar (Windows) or Dock (macOS)
 * - Application continues running in the background
 * - Window restores when user clicks the taskbar/dock icon
 *
 * @see rememberAppMinimizer Factory that sets up window state binding
 */
actual class AppMinimizer {
    private var windowState: WindowState? = null
    private var frame: Frame? = null

    /**
     * Sets the Compose [WindowState] for minimization control.
     *
     * @param state The window state from [rememberWindowState]
     */
    fun setWindow(state: WindowState) {
        windowState = state
    }

    /**
     * Sets the AWT [Frame] for direct window control.
     *
     * @param frame The AWT frame underlying the Compose window
     */
    fun setFrame(frame: Frame) {
        this.frame = frame
    }

    /**
     * Minimizes the desktop window.
     *
     * Sets both the Compose [WindowState.isMinimized] flag and the AWT
     * [Frame.ICONIFIED] state to ensure the window minimizes regardless
     * of how the window was created.
     */
    actual fun minimize() {
        windowState?.isMinimized = true
        // Alternative using AWT Frame directly
        frame?.state = Frame.ICONIFIED
    }
}