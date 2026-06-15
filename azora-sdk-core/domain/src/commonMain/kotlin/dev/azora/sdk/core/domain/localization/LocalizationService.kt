package dev.azora.sdk.core.domain.localization

import dev.azora.shared.data.Locale
import kotlinx.coroutines.flow.StateFlow

/**
 * Service responsible for managing application localization and language preferences.
 *
 * Provides functionality to:
 * - Track the current locale as a reactive state
 * - Persist and retrieve locale preferences
 * - Resolve locale based on user preferences, device settings, or defaults
 */
interface LocalizationService {

    /**
     * Current application locale as a reactive state flow.
     * Observers will be notified whenever the locale changes.
     */
    val currentLocale: StateFlow<Locale>

    /**
     * Sets the application locale.
     *
     * @param code The locale code (e.g., "en", "ro", "fr") to set, or null to clear
     *             the override and fallback to device language.
     */
    suspend fun setLocale(code: String?)

    /**
     * Resolves the appropriate locale based on the given saved code,
     * device language, or fallback defaults.
     *
     * @param savedCode The previously saved locale code, if any.
     * @return The resolved [Locale] instance.
     */
    fun resolveLocale(savedCode: String?): Locale
}