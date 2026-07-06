package dev.azora.sdk.library.presentation

import dev.azora.sdk.core.project.domain.ProjectTemplateContribution
import dev.azora.sdk.library.core.InstalledLibrary
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic library manager.
 *
 * Libraries are versioned bundles (e.g. the Azora Engine) installed under
 * `~/.azora/libraries/<id>/<version>/` and described by a `library.json`
 * manifest. They are purely data-driven: the manager reads manifests and
 * exposes the templates they contribute; no code from the bundle is loaded
 * into the Studio process.
 *
 * The desktop implementation installs bundles from a directory or a
 * `.azlib`/`.zip` archive.
 */
interface LibraryManager {

    /** All installed libraries (latest version per id). */
    val installedLibraries: StateFlow<List<InstalledLibrary>>

    /** Scans the library directory and refreshes [installedLibraries]. */
    suspend fun loadInstalledLibraries()

    /**
     * Installs a library bundle from [sourcePath] — either a bundle directory
     * (containing `library.json`) or a `.azlib`/`.zip` archive of one.
     *
     * @return the installed library, or null when the source is not a valid bundle.
     */
    suspend fun installLibrary(sourcePath: String): InstalledLibrary?

    /** Removes an installed library (all versions of [libraryId]). */
    suspend fun uninstallLibrary(libraryId: String)

    /**
     * Project templates contributed by all installed libraries, ready for the
     * create-project UI and the template-generator resolver.
     */
    fun templateContributions(): List<ProjectTemplateContribution>
}
