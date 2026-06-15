package dev.azora.shared.dto.user.request

import kotlinx.serialization.Serializable

/**
 * Request DTO for user account deletion.
 *
 * Requires the user's password as confirmation to prevent accidental
 * or unauthorized account deletions. The password is verified against
 * the stored hash before the deletion is processed.
 *
 * @property password The user's current password for verification
 */
@Serializable
data class DeleteAccountRequest(
    val password: String
)