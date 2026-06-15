package dev.azora.shared.dto.user.response

import kotlinx.serialization.Serializable

/**
 * Response payload for successful authentication operations.
 * Returned after login or registration.
 *
 * @property user The authenticated user's information.
 * @property accessToken JWT access token for authenticating API requests.
 * @property refreshToken JWT refresh token for obtaining new access tokens.
 * @property expiresIn Time in seconds until the access token expires.
 */
@Serializable
data class AuthResponse(
    val user: UserResponse,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)