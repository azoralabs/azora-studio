package dev.azora.sdk.core.presentation.storage.observer

import androidx.compose.runtime.*
import dev.azora.sdk.core.domain.auth.SessionStorage
import dev.azora.sdk.core.presentation.util.collectAsSWLC
import org.koin.compose.koinInject

/**
 * Observes the selected locale from session storage.
 *
 * @return The locale string, or null if not set
 */
@Composable
fun observeLocale(): String? {
    val sessionStorage = koinInject<SessionStorage>()
    val locale by sessionStorage.observeLocale()
        .collectAsSWLC(initialValue = null)

    return locale
}