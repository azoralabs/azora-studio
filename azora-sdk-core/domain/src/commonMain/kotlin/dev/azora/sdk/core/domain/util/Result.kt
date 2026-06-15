package dev.azora.sdk.core.domain.util

/**
 * A discriminated union that represents either success or failure for operations.
 *
 * This type is used throughout the application to handle operations that can fail,
 * such as network requests, database operations, or business logic validation.
 *
 * @param D The type of data returned on success
 * @param E The type of error returned on failure (must extend [Error])
 */
sealed interface Res<out D, out E: Error> {

    /**
     * Represents a successful operation with the resulting data.
     *
     * @property data The successful result value
     */
    data class Success<out D>(val data: D): Res<D, Nothing>

    /**
     * Represents a failed operation with an error.
     *
     * @property error The error that caused the failure
     */
    data class Failure<out E: Error>(val error: E): Res<Nothing, E>
}

/**
 * Transforms the success value of a [Res] using the provided mapping function.
 *
 * If the result is a failure, the error is propagated unchanged.
 *
 * @param map Function to transform the success value
 * @return A new [Res] with the transformed value, or the original failure
 */
inline fun <T, E: Error, R> Res<T, E>.map(map: (T) -> R): Res<R, E> = when (this) {
    is Res.Failure -> Res.Failure(error)
    is Res.Success -> Res.Success(map(this.data))
}

/**
 * Executes the given action if this result is a success.
 *
 * The action receives the success value as a parameter. The original result
 * is returned unchanged to allow chaining.
 *
 * @param action The action to execute with the success value
 * @return The original result for chaining
 */
inline fun <T, E: Error> Res<T, E>.onSuccess(action: (T) -> Unit): Res<T, E> = when (this) {
    is Res.Failure -> this
    is Res.Success -> { action(this.data); this }
}

/**
 * Executes the given action if this result is a failure.
 *
 * The action receives the error as a parameter. The original result
 * is returned unchanged to allow chaining.
 *
 * @param action The action to execute with the error
 * @return The original result for chaining
 */
inline fun <T, E: Error> Res<T, E>.onFailure(action: (E) -> Unit): Res<T, E> = when (this) {
    is Res.Failure -> { action(error); this }
    is Res.Success -> this
}

/**
 * Converts a [Res] into an [EmptyRes] by discarding the success value.
 *
 * This is useful for operations where you only care about success/failure,
 * not the actual data returned.
 *
 * @return An [EmptyRes] with Unit as the success type
 */
fun <T, E: Error> Res<T, E>.asEmptyRes(): EmptyRes<E> = map {}

/**
 * A [Res] type that doesn't carry any data on success.
 *
 * Useful for operations like delete, update, or void functions where
 * you only need to know if the operation succeeded or failed.
 */
typealias EmptyRes<E> = Res<Unit, E>