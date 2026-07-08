package dev.azora.studio.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The open project's per-project plugin enablement, shared between the
 * Settings panel (which edits it) and the main window (which filters the
 * active plugins by it).
 *
 * `null` means the project predates per-project enablement — every installed
 * plugin stays active for backward compatibility. New projects always carry
 * an explicit list (see ProjectManagerViewModel).
 */
class ProjectPluginsState {

    private val _enabledIds = MutableStateFlow<List<String>?>(null)
    val enabledIds: StateFlow<List<String>?> = _enabledIds.asStateFlow()

    fun set(ids: List<String>?) {
        _enabledIds.value = ids
    }

    fun toggle(pluginId: String, enabled: Boolean) {
        val current = _enabledIds.value ?: return
        _enabledIds.value = if (enabled) (current + pluginId).distinct() else current - pluginId
    }
}
