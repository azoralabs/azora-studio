package dev.azora.shared.dto.user.request

import kotlinx.serialization.Serializable

/**
 * Request payload for Google OAuth authentication.
 *
 * This request contains the Google ID token obtained from the Google Sign-In flow
 * on the client side. The server will verify this token with Google's servers
 * and create or update a user account based on the Google account information.
 *
 * @property idToken The Google ID token (JWT) obtained from Google Sign-In
 */
@Serializable
data class GoogleAuthRequest(
    val idToken: String,
)