@file:UseSerializers(InstantSerializer::class)

package dev.azora.shared.dto.user.response

import dev.azora.sdk.core.util.InstantSerializer
import kotlinx.serialization.*
import kotlin.time.Instant

/**
 * Represents a user account in the system.
 * Contains all user profile information, preferences, and metadata.
 *
 * @property id Unique identifier for the user.
 * @property accountId Identifier linking the user to their associated account.
 *
 * @property email The user's email address. Used for authentication and communication.

 * @property verified Indicates whether the user's email/identity has been verified.
 * @property profilePicture Optional Base64-encoded profile picture image data.
 *
 * @property daysTogether Counter tracking the number of days the user has been active or engaged with the platform.
 *
 * @property appNotifications User preference for receiving notifications through the mobile/web application.
 * @property emailNotifications User preference for receiving notifications via email.
 *
 * @property appMarketing User consent for receiving marketing content through the mobile/web application.
 * @property emailMarketing User consent for receiving marketing content via email.
 *
 * @property terms Indicates whether the user has accepted the terms and conditions.
 * @property gdpr Indicates whether the user has consented to GDPR data processing requirements.
 *
 * @property createdAt Timestamp when the user account was created.
 * @property updatedAt Timestamp of the last update to the user account. Null if never updated.
 * @property lastVisit Timestamp of the user's last login or activity. Null if the user has never logged in.
 * @property deletedAt Timestamp when the user account was soft-deleted. Null if the account is not deleted.
 */
@Serializable
data class UserResponse(
    val id: String,
    val accountId: String,

    val email: String,

    val verified: Boolean,
    val profilePicture: String? = null,

    val daysTogether: Int,

    val appNotifications: Boolean,
    val emailNotifications: Boolean,

    val appMarketing: Boolean,
    val emailMarketing: Boolean,

    val terms: Boolean,
    val gdpr: Boolean,

    val createdAt: Instant,
    val updatedAt: Instant?,
    val deletedAt: Instant?,
    val lastVisit: Instant?
)