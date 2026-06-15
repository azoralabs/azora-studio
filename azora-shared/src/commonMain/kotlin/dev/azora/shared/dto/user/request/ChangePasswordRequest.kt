package dev.azora.shared.dto.user.request

import kotlinx.serialization.Serializable

/**
 * Request body for changing a user's password.
 *
 * Sent to the backend to update an authenticated user's password.
 * Requires both the current password for verification and the new password.
 *
 * @property oldPassword The user's current password for verification
 * @property newPassword The new password to set
 */
@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)