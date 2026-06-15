package dev.azora.shared.networking.response

import kotlinx.serialization.Serializable

/**
 * Response data for POST (Create) operations.
 *
 * Used when creating new resources via POST endpoints.
 *
 * @property createdRows Number of rows/records successfully created
 *
 * Example usage:
 * ```
 * POST /api/users
 * Response: ApiResponse<CreateResponse, Nothing>(
 *     success = true,
 *     message = "Users created successfully",
 *     data = CreateResponse(createdRows = 5)
 * )
 * ```
 */
@Serializable
data class CreateResponse(
    val createdRows: Int
)