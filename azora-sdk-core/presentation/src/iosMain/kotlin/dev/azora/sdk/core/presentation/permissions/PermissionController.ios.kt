package dev.azora.sdk.core.presentation.permissions

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS implementation of [PermissionController].
 *
 * - Notifications: requests authorization via [UNUserNotificationCenter] and suspends for the result.
 * - Location: triggers a "when in use" authorization request and maps the current
 *   [CLLocationManager] authorization status.
 */
actual class PermissionController {

    private val locationManager = CLLocationManager()

    actual suspend fun requestPermission(permission: Permission): PermissionState =
        when (permission) {
            Permission.NOTIFICATIONS -> requestNotifications()
            Permission.LOCATION -> requestLocation()
        }

    private suspend fun requestNotifications(): PermissionState =
        suspendCancellableCoroutine { continuation ->
            val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(options) { granted, _ ->
                    if (continuation.isActive) {
                        continuation.resume(if (granted) PermissionState.GRANTED else PermissionState.DENIED)
                    }
                }
        }

    private fun requestLocation(): PermissionState {
        return when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedAlways,
            kCLAuthorizationStatusAuthorizedWhenInUse -> PermissionState.GRANTED

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> PermissionState.PERMANENTLY_DENIED

            kCLAuthorizationStatusNotDetermined -> {
                locationManager.requestWhenInUseAuthorization()
                PermissionState.NOT_DETERMINED
            }

            else -> PermissionState.NOT_DETERMINED
        }
    }
}
