package dev.azora.shared.dto.user.request

import kotlinx.serialization.Serializable

/**
 * Request payload for email-based operations like forgot password.
 *
 * This request contains an email address and is used for operations
 * that need to send emails to users, such as password reset requests.
 *
 * @property email The user's email address
 */
@Serializable
data class EmailRequest(
    val email: String
)