package dev.azora.shared.dto.user.request

import kotlinx.serialization.Serializable

/**
 * Request body for refreshing authentication tokens.
 *
 * Sent to the backend to obtain a new access token using a valid refresh token.
 * Also used during logout to invalidate the refresh token.
 *
 * @property refreshToken The refresh token to use for obtaining a new access token
 */
@Serializable
data class RefreshRequest(
    val refreshToken: String
)