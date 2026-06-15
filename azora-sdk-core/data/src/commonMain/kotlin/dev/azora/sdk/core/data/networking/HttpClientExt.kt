package dev.azora.sdk.core.data.networking

import dev.azora.sdk.core.domain.util.*
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse

/**
 * Executes an HTTP request safely in a platform-specific manner.
 *
 * This function must be implemented for each supported platform (e.g., Android, iOS)
 * using the `expect/actual` mechanism.
 *
 * @param execute A lambda that performs the HTTP request.
 * @param handleResponse A lambda that converts the [HttpResponse] into a [Res].
 * @return A [Res] wrapping the parsed response or a [DataError.Remote] on failure.
 */
expect suspend fun <T> platformSafeCall(
    execute: suspend () -> HttpResponse,
    handleResponse: suspend (HttpResponse) -> Res<T, DataError.Remote>
): Res<T, DataError.Remote>


/**
 * Executes an HTTP request safely using a default [dev.azora.sdk.core.data.networking.responseToRes] handler.
 *
 * This is a cross-platform helper built on top of [dev.azora.sdk.core.data.networking.platformSafeCall].
 *
 * @param execute The request execution block.
 * @return A [Res] containing the parsed response or a [DataError.Remote] failure.
 */
suspend inline fun <reified T> safeCall(
    noinline execute: suspend () -> HttpResponse
): Res<T, DataError.Remote> = platformSafeCall(execute) { response ->
    responseToRes(response)
}

/**
 * Performs an HTTP GET request and parses the response into [Response].
 *
 * @param route The endpoint route or full URL.
 * @param queryParams Optional query parameters.
 * @param builder Additional configuration for the request.
 * @return A [Res] wrapping the parsed [Response] or a [DataError.Remote].
 */
suspend inline fun <reified Response: Any> HttpClient.get(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    crossinline builder: HttpRequestBuilder.() -> Unit = {}
): Res<Response, DataError.Remote> =
    safeCall {
        get {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            builder()
        }
    }

/**
 * Performs an HTTP POST request with a [body] and parses the response into [Response].
 */
suspend inline fun <reified Request, reified Response: Any> HttpClient.post(
    route: String,
    body: Request,
    queryParams: Map<String, Any> = mapOf(),
    crossinline builder: HttpRequestBuilder.() -> Unit = {}
): Res<Response, DataError.Remote> =
    safeCall {
        post {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        }
    }

/**
 * Performs an HTTP PUT request with a [body] and parses the response into [Response].
 */
suspend inline fun <reified Request, reified Response: Any> HttpClient.put(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    body: Request,
    crossinline builder: HttpRequestBuilder.() -> Unit = {}
): Res<Response, DataError.Remote> =
    safeCall {
        put {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        }
    }


/**
 * Performs an HTTP PATCH request with a [body] and parses the response into [Response].
 */
suspend inline fun <reified Request, reified Response: Any> HttpClient.patch(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    body: Request,
    crossinline builder: HttpRequestBuilder.() -> Unit = {}
): Res<Response, DataError.Remote> =
    safeCall {
        patch {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        }
    }

/**
 * Performs an HTTP DELETE request and parses the response into [Response].
 */
suspend inline fun <reified Response: Any> HttpClient.delete(
    route: String,
    queryParams: Map<String, Any> = mapOf(),
    crossinline builder: HttpRequestBuilder.() -> Unit = {}
): Res<Response, DataError.Remote> =
    safeCall {
        delete {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            builder()
        }
    }

/**
 * Performs an HTTP DELETE request with a [body] and parses the response into [Response].
 */
suspend inline fun <reified Request, reified Response: Any> HttpClient.delete(
    route: String,
    body: Request,
    queryParams: Map<String, Any> = mapOf(),
    crossinline builder: HttpRequestBuilder.() -> Unit = {}
): Res<Response, DataError.Remote> =
    safeCall {
        delete {
            url(constructRoute(route))
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
            builder()
        }
    }

/**
 * Converts an [HttpResponse] into a [Res] by mapping status codes to [DataError.Remote].
 *
 * On success (HTTP 2xx), attempts to deserialize the response body into type [T].
 * On failure, returns a matching [DataError.Remote] code or `UNKNOWN` if unrecognized.
 *
 * @param response The [HttpResponse] from the Ktor client.
 * @return A [Res] representing either success with the parsed body or an error.
 */
suspend inline fun <reified T> responseToRes(
    response: HttpResponse
): Res<T, DataError.Remote> = when (response.status.value) {
    in 200..299 -> {
        try {
            Res.Success(response.body<T>())
        } catch (_: NoTransformationFoundException) {
            Res.Failure(DataError.Remote.SERIALIZATION)
        }
    }

    400 -> Res.Failure(DataError.Remote.BAD_REQUEST)
    401 -> Res.Failure(DataError.Remote.UNAUTHORIZED)
    403 -> Res.Failure(DataError.Remote.FORBIDDEN)
    404 -> Res.Failure(DataError.Remote.NOT_FOUND)
    408 -> Res.Failure(DataError.Remote.REQUEST_TIMEOUT)
    409 -> Res.Failure(DataError.Remote.CONFLICT)
    413 -> Res.Failure(DataError.Remote.PAYLOAD_TOO_LARGE)
    429 -> Res.Failure(DataError.Remote.TOO_MANY_REQUESTS)

    500 -> Res.Failure(DataError.Remote.SERVER_ERROR)
    503 -> Res.Failure(DataError.Remote.SERVICE_UNAVAILABLE)

    else -> Res.Failure(DataError.Remote.UNKNOWN)
}

/**
 * Constructs a valid URL route by prepending the [dev.azora.sdk.core.data.networking.UrlConstants.BASE_URL_HTTP]
 * if the given [route] is not already a full URL.
 *
 * @param route The relative or absolute route.
 * @return A fully qualified URL for the request.
 */
fun constructRoute(route: String) = when {
    route.contains(UrlConstants.BASE_URL_HTTP) -> route
    route.startsWith("/") -> "${UrlConstants.BASE_URL_HTTP}$route"
    else -> "${UrlConstants.BASE_URL_HTTP}/$route"
}