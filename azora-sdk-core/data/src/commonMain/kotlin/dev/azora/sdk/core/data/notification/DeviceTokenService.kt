package dev.azora.sdk.core.data.notification

import dev.azora.sdk.core.data.networking.delete
import dev.azora.sdk.core.data.networking.post
import dev.azora.sdk.core.data.networking.*
import dev.azora.sdk.core.domain.notification.DeviceTokenService
import dev.azora.sdk.core.domain.util.*
import dev.azora.shared.dto.device.request.RegisterDeviceTokenRequest
import io.ktor.client.HttpClient

/**
 * Ktor-based implementation of [dev.azora.sdk.core.domain.notification.DeviceTokenService].
 *
 * Handles push notification device token registration and unregistration
 * through HTTP API calls to the backend server.
 *
 * @property httpClient The configured [HttpClient] used for network requests
 */
class KtorDeviceTokenService(
    private val httpClient: HttpClient
): dev.azora.sdk.core.domain.notification.DeviceTokenService {

    /**
     * Registers a device token with the backend for push notifications.
     *
     * Sends a POST request to `/notification/register` with the device token and platform.
     *
     * @param token The platform-specific device token (FCM for Android, APNs for iOS)
     * @param platform Platform identifier (e.g., "android", "ios")
     * @return An [dev.azora.sdk.core.domain.util.EmptyRes] indicating success or failure with [dev.azora.sdk.core.domain.util.DataError.Remote]
     */
    override suspend fun registerToken(
        token: String,
        platform: String
    ): dev.azora.sdk.core.domain.util.EmptyRes<dev.azora.sdk.core.domain.util.DataError.Remote> = httpClient.post(
        route = "/notification/register",
        body = RegisterDeviceTokenRequest(
            token = token,
            platform = platform
        )
    )

    /**
     * Unregisters a device token from the backend.
     *
     * Sends a DELETE request to `/notification/{token}` to remove the token.
     *
     * @param token The device token to unregister
     * @return An [dev.azora.sdk.core.domain.util.EmptyRes] indicating success or failure with [dev.azora.sdk.core.domain.util.DataError.Remote]
     */
    override suspend fun unregisterToken(token: String):
            dev.azora.sdk.core.domain.util.EmptyRes<dev.azora.sdk.core.domain.util.DataError.Remote> = httpClient.delete(
        route = "/notification/$token"
    )
}