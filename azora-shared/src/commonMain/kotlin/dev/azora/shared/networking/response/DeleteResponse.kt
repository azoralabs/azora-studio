package dev.azora.shared.networking.response

import kotlinx.serialization.Serializable

/**
 * Response data for DELETE operations.
 *
 * Used when deleting resources via DELETE endpoints.
 *
 * @property deletedRows Number of rows/records successfully deleted
 *
 * Example usage:
 * ```
 * DELETE /api/users/123
 * DELETE /api/users?ids=1,2,3
 * Response: ApiResponse<DeleteResponse, Nothing>(
 *     success = true,
 *     message = "Users deleted successfully",
 *     data = DeleteResponse(deletedRows = 3)
 * )
 * ```
 */
@Serializable
data class DeleteResponse(
    val deletedRows: Int
)