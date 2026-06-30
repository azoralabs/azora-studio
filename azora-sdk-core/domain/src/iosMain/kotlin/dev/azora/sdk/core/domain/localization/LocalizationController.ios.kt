package dev.azora.sdk.core.domain.localization

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of [LocalizationController].
 *
 * iOS resolves its UI language from `AppleLanguages` in the standard user defaults, which is read
 * at launch. This implementation persists the requested language there so it takes effect on the
 * next app launch; the current session is not re-localized live.
 */
actual class LocalizationController actual constructor() {

    /**
     * No-op on iOS. Returns this instance for chaining.
     */
    actual fun initialize(context: Any?): LocalizationController = this

    /**
     * Persists the preferred language to `AppleLanguages` so it is applied on next launch.
     *
     * @param languageCode ISO 639-1 language code (e.g. "en", "fr")
     */
    actual fun update(languageCode: String) {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.setObject(listOf(languageCode), forKey = "AppleLanguages")
        defaults.synchronize()
    }
}
