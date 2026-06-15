package dev.azora.sdk.core.domain.localization

import java.util.Locale

/**
 * JVM / Desktop implementation of [LocalizationController].
 *
 * Updates the default JVM locale using [Locale.setDefault].
 *
 * This affects locale-sensitive APIs such as:
 * - DateFormat
 * - NumberFormat
 * - Currency formatting
 *
 * Note:
 * - UI frameworks (Compose, Swing, JavaFX) do not automatically recompose or refresh.
 * - Manual recomposition or UI refresh may be required.
 */
actual class LocalizationController actual constructor() {

    /**
     * No-op on desktop. Returns this instance for chaining.
     */
    actual fun initialize(context: Any?): LocalizationController = this

    /**
     * Updates the JVM default locale.
     *
     * @param languageCode ISO 639-1 language code (e.g. "en", "fr")
     */
    actual fun update(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        // Note: Changing the default locale affects JVM functions like DateFormat, NumberFormat
        // but does not automatically update any UI components. Recompose or refresh manually if needed.
    }
}