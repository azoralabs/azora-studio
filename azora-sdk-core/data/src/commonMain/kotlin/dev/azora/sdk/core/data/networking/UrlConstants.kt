package dev.azora.sdk.core.data.networking

/**
 * Centralized constants for backend API URLs.
 *
 * Provides the base HTTP URL for all API endpoints. This constant is used
 * throughout the application to construct full endpoint URLs for network requests.
 *
 * Platform-specific notes:
 * - Web/WASM: Uses empty string for relative URLs (proxied through dev server)
 * - Other platforms: Uses the full backend URL
 */
expect object UrlConstants {

    /**
     * Base HTTP URL for the backend API.
     *
     * All API requests are constructed by appending endpoint paths to this base URL.
     */
    val BASE_URL_HTTP: String
}