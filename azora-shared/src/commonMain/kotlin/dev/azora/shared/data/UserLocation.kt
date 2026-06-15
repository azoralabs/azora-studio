package dev.azora.shared.data

/**
 * Represents a geographic location with latitude and longitude coordinates.
 *
 * @property latitude The latitude coordinate (-90 to 90)
 * @property longitude The longitude coordinate (-180 to 180)
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double
)