package dev.azora.shared.networking.response

import kotlinx.serialization.Serializable

/**
 * Generic API response wrapper for all endpoint responses.
 *
 * @param T Type of the main data payload
 * @param I Type of additional info/metadata (nullable)
 * @property success Indicates whether the operation succeeded (true) or failed (false)
 * @property message Human-readable description of the operation result
 * @property data Main response payload containing the operation result
 * @property info Optional additional metadata or context about the operation
 */
@Serializable
data class ApiResponse<T, I>(
    val success: Boolean,
    val message: String,
    val data: T,
    val info: I? = null
) {

    companion object {

        /**
         * Creates a successful API response.
         *
         * @param message Success message describing the operation
         * @param data The response data payload
         * @param info Optional additional information
         * @return ApiResponse with success=true
         */
        fun <T, I> success(message: String, data: T, info: I? = null): ApiResponse<T, I?> =
            ApiResponse(true, message, data, info)

        /**
         * Creates an error API response.
         *
         * @param message Error message describing what went wrong
         * @param data Optional data payload (typically null for errors)
         * @param info Optional additional error context
         * @return ApiResponse with success=false
         */
        fun <T, I> error(message: String, data: T? = null, info: I? = null): ApiResponse<T?, I?> =
            ApiResponse(false, message, data, info)
    }
}