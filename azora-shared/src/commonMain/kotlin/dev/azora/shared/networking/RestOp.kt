package dev.azora.shared.networking

import dev.azora.shared.networking.response.*

/**
 * REST operation types for API endpoints.
 *
 * Defines the standard HTTP methods and custom operations supported by the API.
 */
enum class RestOp {

    /**
     * GET operation - Retrieve/read resources.
     *
     * Used for fetching data without side effects (idempotent and safe).
     * Should not modify server state.
     *
     * Examples:
     * - GET /api/users - List all users
     * - GET /api/users/123 - Get specific user
     */
    GET,

    /**
     * POST operation - Create new resources.
     *
     * Used for creating new records/entities.
     * Non-idempotent (multiple identical requests create multiple resources).
     *
     * Response: Typically returns [CreateResponse]
     *
     * Examples:
     * - POST /api/users - Create new user(s)
     * - POST /api/orders - Create new order
     */
    POST,

    /**
     * PUT operation - Full update/replace resources.
     *
     * Used for completely replacing an existing resource or creating if it doesn't exist (upsert).
     * Idempotent (multiple identical requests produce the same result).
     *
     * Response: Typically returns [UpdateResponse] or [UpsertResponse]
     *
     * Examples:
     * - PUT /api/users/123 - Replace entire user record
     * - PUT /api/users/sync - Full synchronization
     */
    PUT,

    /**
     * PATCH operation - Partial update of resources.
     *
     * Used for modifying specific fields of an existing resource.
     * May or may not be idempotent depending on implementation.
     *
     * Response: Typically returns [UpdateResponse] or [UpsertResponse]
     *
     * Examples:
     * - PATCH /api/users/123 - Update specific user fields
     * - PATCH /api/users/123/status - Update only status field
     */
    PATCH,

    /**
     * DELETE operation - Remove resources.
     *
     * Used for deleting existing records/entities.
     * Idempotent (deleting an already deleted resource returns the same result).
     *
     * Response: Typically returns [DeleteResponse]
     *
     * Examples:
     * - DELETE /api/users/123 - Delete specific user
     * - DELETE /api/users?ids=1,2,3 - Delete multiple users
     */
    DELETE,

    /**
     * SYNC operation - Custom synchronization operation.
     *
     * Used for complex synchronization workflows that may involve multiple operations
     * (create, update, delete) in a single request to maintain data consistency.
     *
     * Response: Typically returns [UpsertResponse] with all operation counts
     *
     * Examples:
     * - SYNC /api/users - Synchronize user list with provided data
     * - SYNC /api/inventory - Synchronize inventory state
     */
    SYNC
}