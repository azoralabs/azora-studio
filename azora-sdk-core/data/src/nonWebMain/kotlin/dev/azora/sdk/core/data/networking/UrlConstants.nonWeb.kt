package dev.azora.sdk.core.data.networking

/**
 * Non-web platform implementation of URL constants.
 *
 * Uses the full backend URL for direct API access.
 */
actual object UrlConstants {
    /**
     * Base HTTP URL for the backend API.
     */
    actual val BASE_URL_HTTP: String = "http://65.108.246.213:8081"
}
