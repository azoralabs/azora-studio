package dev.azora.sdk.core.data.localization

import dev.azora.sdk.core.domain.auth.SessionStorage
import dev.azora.sdk.core.domain.localization.LocalizationService
import dev.azora.shared.data.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Production implementation of [dev.azora.sdk.core.domain.localization.LocalizationService] that manages application locale
 * with persistence and device language fallback support.
 *
 * Resolution priority:
 * 1. User's saved locale preference (from [dev.azora.sdk.core.domain.auth.SessionStorage])
 * 2. Device's system language
 * 3. Default English locale
 *
 * @param sessionStorage Storage for persisting locale preferences across app sessions.
 * @param deviceLanguageProvider Function that returns the device's current language code.
 * @param updateSystemLocale Callback to update the system-level locale configuration.
 */
class LocalizationServiceImpl(
    private val sessionStorage: dev.azora.sdk.core.domain.auth.SessionStorage,
    private val deviceLanguageProvider: () -> String,
    private val updateSystemLocale: (String) -> Unit
) : dev.azora.sdk.core.domain.localization.LocalizationService {

    private val defaultLocale = Locale.entries.first { it.code == "en" }

    private val localeState = MutableStateFlow(defaultLocale)

    override val currentLocale: StateFlow<Locale> = localeState.asStateFlow()

    init {
        // Observe persisted changes
        CoroutineScope(Dispatchers.Default).launch {
            sessionStorage.observeLocale().collect { code ->
                val chosen = resolveLocale(code)
                localeState.value = chosen

                // Actually update the system locale
                updateSystemLocale(chosen.code)
            }
        }
    }

    /**
     * Sets the application locale and persists the preference.
     *
     * Pass null to clear the user's locale override, which will cause the app to
     * fallback to the device's system language on next resolution.
     *
     * @param code The locale code (e.g., "en", "ro", "fr") to set, or null to clear.
     */
    override suspend fun setLocale(code: String?) {
        sessionStorage.setLocale(code)
        val chosen = resolveLocale(code)
        localeState.value = chosen

        // Actually update the system locale
        updateSystemLocale(chosen.code)
    }

    /**
     * Resolves the appropriate locale using the following priority:
     * 1. Saved locale from user preferences (if valid)
     * 2. Device's system language (if supported)
     * 3. Default English locale
     *
     * @param savedCode The previously saved locale code from storage, if any.
     * @return The resolved [Locale] instance.
     */
    override fun resolveLocale(savedCode: String?): Locale {
        // 1 - saved locale
        savedCode?.let { key ->
            Locale.entries.find { it.code == key }?.let { return it }
        }

        // 2 - fallback to device language
        val deviceLang = deviceLanguageProvider()
            .substringBefore("-")
            .lowercase()

        Locale.entries.find { it.code == deviceLang }?.let { return it }

        // 3 - fallback EN
        return defaultLocale
    }
}