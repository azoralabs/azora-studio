package dev.azora.shared.networking.response

import kotlinx.serialization.Serializable

/**
 * Response data for PUT (Upsert) operations with full synchronization.
 *
 * Used when performing complex upsert operations that may create, update, or delete records
 * to synchronize state (e.g., bulk sync operations).
 *
 * @property createdRows Number of new rows/records created
 * @property updatedRows Number of existing rows/records updated
 * @property skippedRows Number of rows/records skipped (e.g., no changes needed)
 * @property deletedRows Number of rows/records deleted (e.g., removed during sync)
 *
 * Example usage:
 * ```
 * PUT /api/users/sync
 * Response: ApiResponse<UpsertResponse, Nothing>(
 *     success = true,
 *     message = "Synchronization completed",
 *     data = UpsertResponse(createdRows = 5, updatedRows = 10, skippedRows = 2, deletedRows = 3)
 * )
 * ```
 */
@Serializable
data class UpsertResponse(
    val createdRows: Int,
    val updatedRows: Int,
    val skippedRows: Int,
    val deletedRows: Int
)