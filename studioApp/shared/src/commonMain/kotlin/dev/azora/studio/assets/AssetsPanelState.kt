package dev.azora.studio.assets

import androidx.compose.ui.geometry.Offset

/**
 * UI state for the Assets panel.
 */
data class AssetsPanelState(
    val items: List<AssetItem> = emptyList(),
    val expandedFolders: Set<String> = emptySet(),
    val selectedPath: String? = null,
    val contextMenuPosition: Offset? = null,
    val contextMenuTargetPath: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val renamingPath: String? = null
)
