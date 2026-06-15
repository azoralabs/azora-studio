package dev.azora.sdk.core.domain.mapper

import kotlin.time.Instant

/**
 * Converts a [Long] representing epoch milliseconds to [Instant].
 */
fun Long.toDomain(): Instant = Instant.fromEpochMilliseconds(this)

/**
 * Converts a nullable [Long] representing epoch milliseconds to nullable [Instant].
 */
fun Long?.toDomain(): Instant? = this?.let { Instant.fromEpochMilliseconds(it) }