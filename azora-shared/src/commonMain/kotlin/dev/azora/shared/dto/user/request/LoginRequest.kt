package dev.azora.shared.dto.user.request

import kotlinx.serialization.Serializable

/**
 * Request body for user authentication.
 *
 * Contains the credentials required to log in a user via email and password.
 *
 * @property email The user's email address
 * @property password The user's password
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)