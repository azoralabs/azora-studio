package org.azora.studio.editor

import org.azora.sdk.docking.domain.DockAction as DockDomainAction

sealed interface StudioAction {
    /**
     * Wrapper for dock actions.
     */
    data class DockAction(val action: DockDomainAction) : StudioAction
}
