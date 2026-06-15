package dev.azora.sdk.core.domain.notification

import dev.azora.sdk.core.domain.util.DataError
import dev.azora.sdk.core.domain.util.EmptyRes

/**
 * Service interface for managing push notification device tokens.
 *
 * Handles registration and unregistration of device tokens with the backend server,
 * enabling push notifications to be delivered to specific devices.
 *
 * Device tokens are platform-specific identifiers used by push notification services:
 * - Android: Firebase Cloud Messaging (FCM) tokens
 * - iOS: Apple Push Notification service (APNs) tokens
 */
interface DeviceTokenService {

    /**
     * Registers a device token with the backend for push notifications.
     *
     * Associates the provided device token with the current user's account,
     * allowing the server to send push notifications to this specific device.
     *
     * @param token The platform-specific device token (FCM token for Android, APNs token for iOS)
     * @param platform Platform identifier (e.g., "android", "ios")
     * @return An [EmptyRes] representing success or containing a [DataError.Remote] on failure
     */
    suspend fun registerToken(
        token: String,
        platform: String
    ): EmptyRes<DataError.Remote>

    /**
     * Unregisters a device token from the backend.
     *
     * Removes the association between the device token and the user's account,
     * preventing further push notifications from being sent to this device.
     * Typically called during logout or when the user disables notifications.
     *
     * @param token The device token to unregister
     * @return An [EmptyRes] representing success or containing a [DataError.Remote] on failure
     */
    suspend fun unregisterToken(
        token: String
    ): EmptyRes<DataError.Remote>
}