package dev.azora.studio.editor

import dev.azora.sdk.docking.domain.DockAction as DockDomainAction

sealed interface StudioAction {
    /**
     * Wrapper for dock actions.
     */
    data class DockAction(val action: DockDomainAction) : StudioAction
}
