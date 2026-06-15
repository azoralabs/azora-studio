package dev.azora.shared.dto.user.request

import kotlinx.serialization.Serializable

/**
 * Request payload for creating a new user account.
 *
 * @property email The user's email address. Must be unique and valid.
 * @property password The user's password. Should meet security requirements before sending.
 * @property firstName The user's first name.
 * @property lastName The user's last name.
 */
@Serializable
data class CreateUserRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)