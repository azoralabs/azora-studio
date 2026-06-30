package dev.azora.sdk.core.data.networking

import dev.azora.sdk.core.domain.util.*
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import java.net.*
import kotlin.coroutines.coroutineContext

/**
 * JVM-specific implementation of [platformSafeCall].
 *
 * Executes an HTTP request safely, catching common networking and serialization
 * exceptions and converting them into domain-specific [DataError.Remote] results.
 *
 * This implementation ensures that:
 * - Network-related errors (e.g., unreachable host, no internet)
 *   map to [DataError.Remote.NO_INTERNET].
 * - Timeout errors map to [DataError.Remote.REQUEST_TIMEOUT].
 * - Serialization issues map to [DataError.Remote.SERIALIZATION].
 * - Any other unexpected exception maps to [DataError.Remote.UNKNOWN].
 *
 * If the coroutine has been cancelled, the function checks [coroutineContext.ensureActive]
 * before returning, allowing structured concurrency to propagate cancellation correctly.
 *
 * @param execute The suspend function performing the HTTP request (e.g., `client.get()`).
 * @param handleResponse A suspend function that processes the [HttpResponse]
 *                       and maps it into a [Res] of type [T].
 * @return A [Res] containing either a successful response of type [T]
 *         or a [DataError.Remote] describing the failure reason.
 */
actual suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Res<T, DataError.Remote>
): Res<T, DataError.Remote> = try {
    val response = execute()
    handleResponse(response)
} catch (_: UnknownHostException) {
    Res.Failure(DataError.Remote.NO_INTERNET)
} catch (_: UnresolvedAddressException) {
    Res.Failure(DataError.Remote.NO_INTERNET)
} catch (_: ConnectException) {
    Res.Failure(DataError.Remote.NO_INTERNET)
} catch (_: SocketTimeoutException) {
    Res.Failure(DataError.Remote.REQUEST_TIMEOUT)
} catch (_: HttpRequestTimeoutException) {
    Res.Failure(DataError.Remote.REQUEST_TIMEOUT)
} catch (_: SerializationException) {
    Res.Failure(DataError.Remote.SERIALIZATION)
} catch (_: Exception) {
    coroutineContext.ensureActive()
    Res.Failure(DataError.Remote.UNKNOWN)
}