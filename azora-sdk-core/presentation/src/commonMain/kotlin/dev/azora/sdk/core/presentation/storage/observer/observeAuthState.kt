package dev.azora.sdk.core.presentation.storage.observer

import androidx.compose.runtime.*
import dev.azora.sdk.core.domain.auth.SessionStorage
import dev.azora.sdk.core.presentation.util.collectAsSWLC
import dev.azora.shared.dto.user.response.AuthResponse
import org.koin.compose.koinInject

/**
 * Observes the authentication state from session storage.
 *
 * @return The current auth response, or null if not authenticated
 */
@Composable
fun observeAuthState(): AuthResponse? {
    val sessionStorage = koinInject<SessionStorage>()
    val authState by sessionStorage.observeAuthState()
        .collectAsSWLC(initialValue = null)

    return authState
}