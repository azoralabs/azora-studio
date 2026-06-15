package dev.azora.shared.dto.user.request

import kotlinx.serialization.Serializable

/**
 * Request payload for updating user information.
 * All fields are optional, allowing partial updates.
 *
 * @property email Updated email address. Must be unique if provided.
 * @property profilePicture Updated Base64-encoded profile picture image data.
 * @property removeProfilePicture Flag to remove the existing profile picture.
 * When true, clears the profile picture.
 */
@Serializable
data class UpdateUserRequest(
    val email: String? = null,
    val profilePicture: String? = null,
    val removeProfilePicture: Boolean? = null
)