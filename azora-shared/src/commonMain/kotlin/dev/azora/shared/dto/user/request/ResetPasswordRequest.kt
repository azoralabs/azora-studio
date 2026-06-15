package dev.azora.shared.dto.user.request

import kotlinx.serialization.Serializable

/**
 * Request payload for resetting a user's password.
 *
 * This request contains the new password and the reset token that was
 * sent to the user's email address. The server will verify the token
 * before allowing the password to be changed.
 *
 * @property newPassword The new password to set for the user
 * @property token The password reset token received via email
 */
@Serializable
data class ResetPasswordRequest(
    val newPassword: String,
    val token: String
)