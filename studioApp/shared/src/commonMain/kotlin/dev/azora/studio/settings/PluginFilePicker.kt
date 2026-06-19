package dev.azora.studio.settings

/**
 * Platform file picker used to choose a plugin JAR to install.
 *
 * Only the desktop target provides a real implementation (AWT [java.awt.FileDialog]); on other
 * targets it is unbound, so the Install button is hidden via `getOrNull`.
 */
interface PluginFilePicker {

    /** Opens a native "select JAR" dialog (blocking on desktop) and returns the absolute path, or null if cancelled. */
    fun pickPluginJar(): String?
}
