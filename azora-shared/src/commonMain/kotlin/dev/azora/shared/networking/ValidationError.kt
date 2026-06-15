package dev.azora.shared.networking

import kotlinx.serialization.Serializable

/**
 * Represents a validation error for a specific property.
 *
 * @property property The name of the property that failed validation
 * @property message The validation error message or constraint name
 */
@Serializable
data class ValidationError(
    val property: String,
    val message: String
)