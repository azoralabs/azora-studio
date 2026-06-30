package dev.azora.sdk.core.data.networking

import dev.azora.sdk.core.domain.util.DataError
import dev.azora.sdk.core.domain.util.Res
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import kotlin.coroutines.coroutineContext

/**
 * iOS (Kotlin/Native) implementation of [platformSafeCall].
 *
 * Maps networking and serialization failures into domain-specific [DataError.Remote] results,
 * preserving coroutine cancellation via [ensureActive].
 */
actual suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Res<T, DataError.Remote>
): Res<T, DataError.Remote> = try {
    val response = execute()
    handleResponse(response)
} catch (_: UnresolvedAddressException) {
    Res.Failure(DataError.Remote.NO_INTERNET)
} catch (_: HttpRequestTimeoutException) {
    Res.Failure(DataError.Remote.REQUEST_TIMEOUT)
} catch (_: SerializationException) {
    Res.Failure(DataError.Remote.SERIALIZATION)
} catch (e: Exception) {
    coroutineContext.ensureActive()
    Res.Failure(DataError.Remote.UNKNOWN)
}
