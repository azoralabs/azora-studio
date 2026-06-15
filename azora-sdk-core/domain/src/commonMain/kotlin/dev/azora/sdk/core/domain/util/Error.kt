package dev.azora.sdk.core.domain.util

/**
 * Marker interface for all error types in the application.
 *
 * All error types that can be used with [Res] must implement this interface.
 * This provides a common contract for error handling throughout the application.
 *
 * Implementations include [DataError] for data layer errors and domain-specific
 * error types for business logic validation failures.
 */
interface Error