package dev.azora.sdk.core.project.domain.website

import kotlinx.serialization.Serializable

@Serializable
enum class HttpMethod { GET, POST, PUT, PATCH, DELETE }

/**
 * A single HTTP endpoint the site can call. The generator turns each endpoint into a typed,
 * suspending function on the generated `ApiClient`.
 *
 * @property id Stable id referenced by [CallApiAction.endpointId].
 * @property name Function name on the generated client (sanitized to a valid Kotlin identifier).
 * @property method HTTP verb.
 * @property path Path appended to [WebsiteModel]'s `apiBaseUrl` (may start with `/`).
 * @property headers Static request headers.
 * @property requestType Optional Kotlin type name for the request body (POST/PUT/PATCH).
 * @property responseType Kotlin type name the JSON response decodes to (defaults to `String`).
 */
@Serializable
data class ApiEndpoint(
    val id: String = randomComponentId(),
    val name: String,
    val method: HttpMethod = HttpMethod.GET,
    val path: String = "/",
    val headers: Map<String, String> = emptyMap(),
    val requestType: String? = null,
    val responseType: String = "String"
)
