package dev.azora.sdk.core.presentation.storage.observer

import androidx.compose.runtime.*
import dev.azora.sdk.core.domain.auth.SessionStorage
import dev.azora.sdk.core.presentation.util.collectAsSWLC
import org.koin.compose.koinInject

/**
 * Observes whether the user is currently authenticated.
 *
 * @return true if authenticated, false otherwise
 */
@Composable
fun observeIsAuthenticated(): Boolean {
    val sessionStorage = koinInject<SessionStorage>()
    val authState by sessionStorage.observeAuthState()
        .collectAsSWLC(initialValue = null)

    return authState != null
}