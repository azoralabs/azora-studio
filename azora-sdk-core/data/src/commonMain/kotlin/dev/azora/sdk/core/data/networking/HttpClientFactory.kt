package dev.azora.sdk.core.data.networking

import dev.azora.sdk.core.data.networking.post
import dev.azora.BuildConfig
import dev.azora.sdk.core.domain.auth.SessionStorage
import dev.azora.sdk.core.domain.logging.AzoraLogger
import dev.azora.sdk.core.domain.util.onFailure
import dev.azora.sdk.core.domain.util.onSuccess
import dev.azora.sdk.core.domain.util.*
import dev.azora.shared.dto.user.request.RefreshRequest
import dev.azora.shared.dto.user.response.AuthResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

/**
 * Factory responsible for creating configured instances of [HttpClient].
 *
 * The [HttpClientFactory] sets up all core features required for network communication,
 * such as content negotiation, authentication, timeouts, logging, and WebSocket support.
 *
 * @property genericLogger Used to log HTTP activity and debugging information.
 * @property sessionStorage Provides access to authentication tokens and session data.
 */
class HttpClientFactory(
    private val genericLogger: dev.azora.sdk.core.domain.logging.AzoraLogger,
    private val sessionStorage: dev.azora.sdk.core.domain.auth.SessionStorage,
) {

    /**
     * Creates a configured [HttpClient] instance using the given [engine].
     *
     * The created client includes:
     * - JSON serialization with `ignoreUnknownKeys = true`
     * - Standard request and socket timeouts
     * - Request/response logging via [genericLogger]
     * - WebSocket support with a 20-second ping interval
     * - Bearer token authentication with automatic refresh using [sessionStorage]
     * - Default API headers (API key and JSON content type)
     *
     * @param engine The [HttpClientEngine] to use (e.g., CIO, Darwin, OkHttp).
     * @return A configured [HttpClient] instance.
     */
    fun create(engine: HttpClientEngine) = HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                json = Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                }
            )
        }
        install(HttpTimeout) {
            socketTimeoutMillis = 20_000L
            requestTimeoutMillis = 20_000L
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    genericLogger.debug(message)
                }
            }
            level = LogLevel.ALL
        }
        install(WebSockets) {
            pingIntervalMillis = 20_000L
        }
        defaultRequest {
            header("x-api-key", BuildConfig.API_KEY)
            contentType(ContentType.Application.Json)
        }

        install(Auth) {
            bearer {
                loadTokens {
                    sessionStorage
                        .observeAuthState()
                        .firstOrNull()
                        ?.let {
                            BearerTokens(
                                accessToken = it.accessToken,
                                refreshToken = it.refreshToken
                            )
                        }
                }
                refreshTokens {
                    val path = response.request.url.encodedPath

                    // Skip token refresh for auth endpoints and account deletion
                    // Account deletion may return 401 for wrong password, not expired token
                    if (path.contains("auth/") ||
                        (path.contains("/users/me") && response.request.method.value == "DELETE")) {
                        return@refreshTokens null
                    }

                    val authState = sessionStorage.observeAuthState().firstOrNull()
                    if (authState?.refreshToken.isNullOrBlank()) {
                        sessionStorage.setAuthState(null)
                        return@refreshTokens null
                    }

                    var bearerTokens: BearerTokens? = null
                    client.post<RefreshRequest, AuthResponse>(
                        route = "/auth/refresh",
                        body = RefreshRequest(
                            refreshToken = authState.refreshToken
                        ),
                        builder = {
                            markAsRefreshTokenRequest()
                        }
                    ).onSuccess { newAuthState ->
                        sessionStorage.setAuthState(newAuthState)
                        bearerTokens = BearerTokens(
                            accessToken = newAuthState.accessToken,
                            refreshToken = newAuthState.refreshToken
                        )
                    }.onFailure { error ->
                        sessionStorage.setAuthState(null)
                    }

                    bearerTokens
                }
            }
        }
    }
}