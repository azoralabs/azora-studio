package dev.azora.shared.networking.response

import kotlinx.serialization.Serializable

/**
 * Response data for PATCH (Update) operations.
 *
 * Used when updating existing resources via PATCH endpoints.
 *
 * @property updatedRows Number of rows/records successfully updated
 * @property skippedRows Number of rows/records that were skipped (e.g., no changes detected, validation failed)
 *
 * Example usage:
 * ```
 * PATCH /api/users/123
 * Response: ApiResponse<UpdateResponse, Nothing>(
 *     success = true,
 *     message = "Users updated successfully",
 *     data = UpdateResponse(updatedRows = 3, skippedRows = 2)
 * )
 * ```
 */
@Serializable
data class UpdateResponse(
    val updatedRows: Int,
    val skippedRows: Int
)