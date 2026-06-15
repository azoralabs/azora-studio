@file:UseSerializers(InstantSerializer::class)

package dev.azora.shared.dto.user.response

import dev.azora.sdk.core.util.InstantSerializer
import kotlinx.serialization.*
import kotlin.time.*

/**
 * Represents a refresh token used for obtaining new access tokens without re-authentication.
 *
 * Refresh tokens are long-lived credentials that allow clients to obtain new access tokens
 * after the original access token expires, without requiring the user to log in again.
 * They should be stored securely and can be revoked if compromised.
 *
 * @property id Unique identifier for this refresh token
 * @property userId The ID of the user this refresh token belongs to
 * @property token The actual refresh token string (should be cryptographically secure)
 * @property revoked Whether this token has been revoked and can no longer be used
 * @property createdAt [Instant] when this refresh token was created
 * @property expiresAt [Instant] when this refresh token expires and becomes invalid
 * @property revokedAt [Instant] when this token was revoked, null if not revoked
 */
@Serializable
data class RefreshTokenResponse(
    val id: String,
    val userId: String,

    val token: String,
    val revoked: Boolean = false,

    val createdAt: Instant,
    val expiresAt: Instant?,
    val revokedAt: Instant? = null
) {

    /**
     * Checks if this refresh token is invalid and cannot be used.
     *
     * A token is considered invalid if:
     * - It has been explicitly revoked, OR
     * - It has expired (current time is past the expiration date)
     *
     * @return `true` if the token is revoked or expired, `false` otherwise
     */
    fun isInvalid() = revoked || (expiresAt?.let { it < Clock.System.now() } ?: false)

    /**
     * Checks if this refresh token is valid and can be used to obtain new access tokens.
     *
     * A token is considered valid if it is neither revoked nor expired.
     * This is the logical inverse of [isInvalid].
     *
     * @return `true` if the token is not revoked and not expired, `false` otherwise
     */
    fun isValid() = !isInvalid()
}