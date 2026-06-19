package dev.azora.sdk.core.project.data.generator.website

import dev.azora.sdk.core.project.domain.website.ApiEndpoint
import dev.azora.sdk.core.project.domain.website.HttpMethod
import dev.azora.sdk.core.project.domain.website.identifier
import dev.azora.sdk.core.project.domain.website.quote

/**
 * Emits `api/ApiClient.kt`: a suspending HTTP client with one typed function per
 * [ApiEndpoint][dev.azora.sdk.core.project.domain.website.ApiEndpoint], built on the browser
 * `fetch` API and kotlinx-serialization for non-`String` payloads.
 *
 * Emits nothing when the model declares no endpoints.
 */
class ApiClientEmitter : KobwebEmitter {

    override fun emit(ctx: KobwebGenContext): List<GeneratedSource> {
        val endpoints = ctx.model.apiEndpoints
        if (endpoints.isEmpty()) return emptyList()

        val needsJson = endpoints.any { it.responseType != "String" || it.requestType != null }
        val needsHeaders = endpoints.any { it.headers.isNotEmpty() }
        val needsRequestInit = endpoints.any { ep ->
            ep.method != HttpMethod.GET || ep.headers.isNotEmpty() ||
                (ep.requestType != null && ep.method in setOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH))
        }

        val code = buildSource {
            packageLine("${ctx.pkg}.api")
            imports(
                *buildList {
                    add("kotlinx.browser.window")
                    add("kotlinx.coroutines.await")
                    if (needsJson) {
                        add("kotlinx.serialization.decodeFromString")
                        add("kotlinx.serialization.encodeToString")
                        add("kotlinx.serialization.json.Json")
                    }
                    if (needsHeaders) add("org.w3c.fetch.Headers")
                    if (needsRequestInit) add("org.w3c.fetch.RequestInit")
                }.sorted().toTypedArray()
            )

            write("/** Generated HTTP client. One suspending function per configured endpoint. */")
            write("class ApiClient(private val baseUrl: String = ${ctx.model.apiBaseUrl.quote()}) {")
            gen {
                endpoints.forEachIndexed { index, endpoint ->
                    if (index > 0) blank()
                    emitEndpoint(this, endpoint)
                }
            }
            write("}")
        }

        return listOf(GeneratedSource("api/ApiClient.kt", code))
    }

    private fun emitEndpoint(
        scope: dev.azora.sdk.core.project.domain.CodeGenerator.GenScope,
        endpoint: ApiEndpoint
    ) = with(scope) {
        val fn = endpoint.name.identifier()
        val hasBody = endpoint.requestType != null &&
            endpoint.method in setOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH)
        val params = if (hasBody) "body: ${endpoint.requestType}" else ""

        write("suspend fun $fn($params): ${endpoint.responseType} {")
        gen {
            write("val url = baseUrl + ${endpoint.path.quote()}")

            val needsInit = endpoint.method != HttpMethod.GET || endpoint.headers.isNotEmpty() || hasBody
            if (endpoint.headers.isNotEmpty()) {
                write("val headers = Headers()")
                endpoint.headers.forEach { (k, v) ->
                    write("headers.append(${k.quote()}, ${v.quote()})")
                }
            }
            if (hasBody) {
                write("val payload = Json.encodeToString(body)")
            }

            if (needsInit) {
                write("val init = RequestInit(")
                gen {
                    val initArgs = buildList {
                        add("method = ${endpoint.method.name.quote()}")
                        if (endpoint.headers.isNotEmpty()) add("headers = headers")
                        if (hasBody) add("body = payload")
                    }
                    initArgs.forEachIndexed { i, arg ->
                        write(if (i == initArgs.lastIndex) arg else "$arg,")
                    }
                }
                write(")")
                write("val response = window.fetch(url, init).await()")
            } else {
                write("val response = window.fetch(url).await()")
            }

            write("val text = response.text().await()")
            if (endpoint.responseType == "String") {
                write("return text")
            } else {
                write("return Json.decodeFromString<${endpoint.responseType}>(text)")
            }
        }
        write("}")
    }
}
