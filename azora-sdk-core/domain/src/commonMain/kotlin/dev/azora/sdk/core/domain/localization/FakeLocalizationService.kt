package dev.azora.sdk.core.domain.localization

import dev.azora.shared.data.Locale
import kotlinx.coroutines.flow.*

/**
 * Fake implementation of [LocalizationService] for testing and preview purposes.
 *
 * Provides a simple, in-memory localization service with a fixed initial locale.
 * This implementation does not persist locale changes and does not interact with
 * platform-specific storage or system settings.
 *
 * Typical use cases include:
 * - Unit and integration testing
 * - Previews where dependency injection is required
 * - Scenarios where a predictable and isolated localization state is desired
 */
class FakeLocalizationService private constructor(
    initialLocale: Locale
) : LocalizationService {

    /**
     * Internal mutable state holding the current locale.
     *
     * This state is updated when [setLocale] is called with a valid locale code.
     */
    private val _currentLocale = MutableStateFlow(initialLocale)

    /**
     * Observable stream of the current locale.
     *
     * Exposed as a read-only [kotlinx.coroutines.flow.StateFlow] to prevent external mutation.
     */
    override val currentLocale: StateFlow<Locale> = _currentLocale.asStateFlow()

    /**
     * Updates the current locale if a valid locale code is provided.
     *
     * If the provided [code] matches one of the supported locales,
     * the current locale state is updated accordingly.
     *
     * Invalid or `null` codes are ignored.
     *
     * Changes are not persisted and only affect this instance.
     *
     * @param code ISO 639-1 language code to switch to, or `null`
     */
    override suspend fun setLocale(code: String?) {
        code?.let {
            Locale.entries.firstOrNull { it.code == code }?.let {
                _currentLocale.value = it
            }
        }
    }

    /**
     * Resolves a locale based on a previously saved locale code.
     *
     * If the provided [savedCode] matches a supported locale,
     * that locale is returned. Otherwise, English is used as a fallback.
     *
     * @param savedCode Previously stored ISO 639-1 language code
     * @return Resolved [Locale], or [Locale.EN] if no match is found
     */
    override fun resolveLocale(savedCode: String?) = savedCode?.let {
        Locale.entries.firstOrNull { it.code == savedCode }
    } ?: Locale.EN

    companion object {

        /**
         * Creates a fake localization service with German locale.
         */
        fun de() = FakeLocalizationService(Locale.entries.first { it.code == "de" })

        /**
         * Creates a fake localization service with English locale.
         */
        fun en() = FakeLocalizationService(Locale.entries.first { it.code == "en" })

        /**
         * Creates a fake localization service with French locale.
         */
        fun fr() = FakeLocalizationService(Locale.entries.first { it.code == "fr" })

        /**
         * Creates a fake localization service with Hungarian locale.
         */
        fun hu() = FakeLocalizationService(Locale.entries.first { it.code == "hu" })

        /**
         * Creates a fake localization service with Italian locale.
         */
        fun it() = FakeLocalizationService(Locale.entries.first { it.code == "it" })

        /**
         * Creates a fake localization service with Spanish locale.
         */
        fun es() = FakeLocalizationService(Locale.entries.first { it.code == "es" })

        /**
         * Creates a fake localization service with Portuguese locale.
         */
        fun pt() = FakeLocalizationService(Locale.entries.first { it.code == "pt" })

        /**
         * Creates a fake localization service with Dutch locale.
         */
        fun nl() = FakeLocalizationService(Locale.entries.first { it.code == "nl" })

        /**
         * Creates a fake localization service with Swedish locale.
         */
        fun sv() = FakeLocalizationService(Locale.entries.first { it.code == "sv" })

        /**
         * Creates a fake localization service with Romanian locale.
         */
        fun ro() = FakeLocalizationService(Locale.entries.first { it.code == "ro" })
    }
}