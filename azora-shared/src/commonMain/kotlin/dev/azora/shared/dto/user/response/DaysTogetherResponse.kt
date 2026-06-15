package dev.azora.shared.dto.user.response

import kotlinx.serialization.Serializable

/**
 * Response payload containing the number of days since user registration.
 *
 * @property daysTogether The number of days since the user created their account.
 */
@Serializable
data class DaysTogetherResponse(
    val daysTogether: Int
)