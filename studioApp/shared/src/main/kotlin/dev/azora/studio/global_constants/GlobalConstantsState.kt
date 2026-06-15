package org.azora.studio.global_constants

import org.azora.sdk.core.project.domain.GlobalConstant

/**
 * UI state for the global constants panel.
 */
data class GlobalConstantsState(
    val isLoading: Boolean = true,
    val constants: List<GlobalConstant> = emptyList(),
    val selectedId: String? = null
)
