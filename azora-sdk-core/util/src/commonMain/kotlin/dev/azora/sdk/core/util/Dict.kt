package dev.azora.sdk.core.util

/**
 * A type alias for Map with String keys.
 * Provides a more semantic name when working with string-keyed maps.
 *
 * @param T the type of values in the dict
 */
typealias Dict<T> = Map<String, T>

/**
 * A type alias for MutableMap with String keys.
 * Provides a more semantic name when working with mutable string-keyed maps.
 *
 * @param T the type of values in the dict
 */
typealias MutableDict<T> = MutableMap<String, T>

/**
 * Returns an empty read-only dict.
 *
 * Example:
 * ```
 * val dict: Dict<Int> = emptyDict()
 * ```
 */
fun <T> emptyDict(): Dict<T> = emptyMap()

/**
 * Returns a new read-only dict with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 *
 * Example:
 * ```
 * val dict = dictOf("name" to "Gabriel", "role" to "Developer")
 * ```
 */
fun <T> dictOf(vararg pairs: Pair<String, T>): Dict<T> = mapOf(*pairs)

/**
 * Returns a new read-only dict with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 *
 * Example:
 * ```
 * val pairs = listOf("name" to "Gabriel", "role" to "Developer")
 * val dict = dictOf(pairs)
 * ```
 */
fun <T> dictOf(pairs: Iterable<Pair<String, T>>): Dict<T> = pairs.toMap()

/**
 * Returns an empty mutable dict.
 *
 * Example:
 * ```
 * val dict = mutableDictOf<Int>()
 * dict["count"] = 42
 * ```
 */
fun <T> mutableDictOf(): MutableDict<T> = mutableMapOf()

/**
 * Returns a new mutable dict with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 *
 * Example:
 * ```
 * val dict = mutableDictOf("name" to "Gabriel", "role" to "Developer")
 * dict["location"] = "Romania"
 * ```
 */
fun <T> mutableDictOf(vararg pairs: Pair<String, T>): MutableDict<T> = mutableMapOf(*pairs)

/**
 * Returns a new mutable dict with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 *
 * Example:
 * ```
 * val pairs = listOf("name" to "Gabriel", "role" to "Developer")
 * val dict = mutableDictOf(pairs)
 * ```
 */
fun <T> mutableDictOf(pairs: Iterable<Pair<String, T>>): MutableDict<T> = pairs.toMap().toMutableMap()

/**
 * Creates a new read-only dict by populating a mutable dict using the given [builderAction]
 * and returning a read-only dict with the same key-value pairs.
 *
 * Example:
 * ```
 * val dict = buildDict<String> {
 *     put("name", "Gabriel")
 *     put("role", "Developer")
 * }
 * ```
 */
inline fun <T> buildDict(builderAction: MutableDict<T>.() -> Unit): Dict<T> {
    return mutableDictOf<T>().apply(builderAction)
}

/**
 * Converts this Iterable of pairs to a Dict.
 *
 * Example:
 * ```
 * val pairs = listOf("name" to "Gabriel", "role" to "Developer")
 * val dict = pairs.toDict()
 * ```
 */
fun <T> Iterable<Pair<String, T>>.toDict(): Dict<T> = toMap()

/**
 * Converts this Iterable of pairs to a MutableDict.
 *
 * Example:
 * ```
 * val pairs = listOf("name" to "Gabriel", "role" to "Developer")
 * val dict = pairs.toMutableDict()
 * ```
 */
fun <T> Iterable<Pair<String, T>>.toMutableDict(): MutableDict<T> = toMap().toMutableMap()

/**
 * Converts this Map to a Dict.
 * This is essentially a type cast for semantic clarity.
 */
fun <T> Map<String, T>.toDict(): Dict<T> = this

/**
 * Converts this Map to a MutableDict.
 */
fun <T> Map<String, T>.toMutableDict(): MutableDict<T> = toMutableMap()