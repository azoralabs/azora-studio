package dev.azora.local.database

import androidx.room.TypeConverter
import kotlinx.serialization.json.*

/**
 * Type converters used by Room to handle custom types in the database.
 *
 * Currently provides conversions for:
 * - [JsonObject] ⇆ [String] (stored as JSON text in the database)
 */
class DatabaseConverters {

    /**
     * Converts a [JsonObject] to its JSON string representation
     * for storing in the database.
     */
    @TypeConverter
    fun fromJsonObject(value: JsonObject?) = value?.toString()

    /**
     * Converts a JSON string from the database back into a [JsonObject].
     */
    @TypeConverter
    fun toJsonObject(value: String?) = value?.let { Json.parseToJsonElement(it).jsonObject }
}