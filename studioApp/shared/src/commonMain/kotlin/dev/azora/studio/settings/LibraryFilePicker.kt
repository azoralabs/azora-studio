package dev.azora.studio.settings

/**
 * Platform picker used to choose a library bundle to install — either a
 * bundle directory (containing `library.json`) or a `.azlib`/`.zip` archive.
 *
 * Only the desktop target provides a real implementation; on other targets it
 * is unbound, so the Install button is hidden via `getOrNull`.
 */
interface LibraryFilePicker {

    /** Opens a native picker (blocking on desktop) and returns the absolute path, or null if cancelled. */
    fun pickLibraryBundle(): String?
}
