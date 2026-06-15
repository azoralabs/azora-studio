package dev.azora.sdk.core.util

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.time.Instant

/**
 * Custom serializer for [Instant], encoding it as an ISO-8601 string.
 *
 * Example JSON:
 * ```json
 * { "instant": "2025-03-15T12:00:00Z" }
 * ```
 */
object InstantSerializer : KSerializer<Instant> {

    override val descriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeString("$value")

    override fun deserialize(decoder: Decoder) =
        Instant.parse(decoder.decodeString())
}

/**
 * A serializable alias for [Instant] using [InstantSerializer].
 */
typealias SerializableInstant = @Serializable(InstantSerializer::class) Instant

/** The default zero-value instant (`1970-01-01T00:00Z`). */
val defaultInstant: SerializableInstant = Instant.fromEpochSeconds(0)