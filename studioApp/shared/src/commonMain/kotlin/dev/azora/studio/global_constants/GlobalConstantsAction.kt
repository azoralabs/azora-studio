package dev.azora.studio.global_constants

import dev.azora.sdk.core.project.domain.ConstantType
import dev.azora.sdk.core.project.domain.GlobalConstant

/**
 * Actions that can be performed on global constants.
 */
sealed interface GlobalConstantsAction {
    data class Add(val type: ConstantType) : GlobalConstantsAction
    data class Update(val constant: GlobalConstant) : GlobalConstantsAction
    data class Remove(val constantId: String) : GlobalConstantsAction
    data class Select(val constantId: String?) : GlobalConstantsAction
}
