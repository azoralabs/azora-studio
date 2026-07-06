package dev.azora.sdk.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.azora.sdk.library.core.InstalledLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI state of the Libraries management screen. */
data class LibraryManagerState(
    val installedLibraries: List<InstalledLibrary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * ViewModel for the Libraries management UI (Project Browser → Libraries and
 * Settings → Libraries).
 */
class LibraryManagerViewModel(
    private val libraryManager: LibraryManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryManagerState())
    val state: StateFlow<LibraryManagerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            libraryManager.installedLibraries.collect { libraries ->
                _state.update { it.copy(installedLibraries = libraries, isLoading = false) }
            }
        }
    }

    fun loadLibraries() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                libraryManager.loadInstalledLibraries()
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load libraries: ${e.message}") }
            }
        }
    }

    fun installLibrary(sourcePath: String) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            val installed = try {
                libraryManager.installLibrary(sourcePath)
            } catch (e: Exception) {
                null
            }
            if (installed == null) {
                _state.update {
                    it.copy(error = "Not a valid library bundle (expected a folder or .azlib with library.json).")
                }
            }
        }
    }

    fun uninstallLibrary(libraryId: String) {
        viewModelScope.launch {
            try {
                libraryManager.uninstallLibrary(libraryId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to remove library: ${e.message}") }
            }
        }
    }
}
