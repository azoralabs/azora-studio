package dev.azora.sdk.core.theme.palette

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Kotlinx Serialization serializer for Compose [Color] values.
 *
 * Serializes colors as their raw Long value representation, enabling
 * [Palette] and [AzoraPalette] to be serialized/deserialized for
 * storage or network transmission.
 *
 * Usage:
 * ```kotlin
 * @file:UseSerializers(ColorSerializer::class)
 * ```
 */
object ColorSerializer : KSerializer<Color> {

    /**
     * Describes the serialized form as a primitive Long.
     */
    override val descriptor =
        PrimitiveSerialDescriptor("Color", PrimitiveKind.LONG)

    /**
     * Serializes a [Color] to its Long representation.
     *
     * @param encoder The encoder to write to.
     * @param value The color to serialize.
     */
    override fun serialize(encoder: Encoder, value: Color) =
        encoder.encodeLong(value.value.toLong())

    /**
     * Deserializes a Long back to a [Color].
     *
     * @param decoder The decoder to read from.
     * @return The deserialized color.
     */
    override fun deserialize(decoder: Decoder) =
        Color(decoder.decodeLong().toULong())
}