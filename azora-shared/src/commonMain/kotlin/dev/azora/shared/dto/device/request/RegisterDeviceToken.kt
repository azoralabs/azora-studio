package dev.azora.shared.dto.device.request

import kotlinx.serialization.Serializable

/**
 * Request body for registering a push notification device token.
 *
 * Sent to the backend to associate a device token with the user's account,
 * enabling push notifications to be delivered to this specific device.
 *
 * @property token The platform-specific device token (FCM for Android, APNs for iOS)
 * @property platform Platform identifier (e.g., "android", "ios")
 */
@Serializable
data class RegisterDeviceTokenRequest(
    val token: String,
    val platform: String
)