package dev.azora.sdk.library.presentation

import dev.azora.sdk.core.project.domain.ProjectTemplateContribution
import dev.azora.sdk.library.core.InstalledLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * No-op [LibraryManager] for platforms without local library support
 * (mobile/web). Reports no installed libraries and rejects installs.
 */
class NoOpLibraryManager : LibraryManager {

    private val _installedLibraries = MutableStateFlow<List<InstalledLibrary>>(emptyList())
    override val installedLibraries: StateFlow<List<InstalledLibrary>> = _installedLibraries.asStateFlow()

    override suspend fun loadInstalledLibraries() {}

    override suspend fun installLibrary(sourcePath: String): InstalledLibrary? = null

    override suspend fun uninstallLibrary(libraryId: String) {}

    override fun templateContributions(): List<ProjectTemplateContribution> = emptyList()
}
