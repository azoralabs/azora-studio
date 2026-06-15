package dev.azora.sdk.core.domain.localization

/**
 * Platform-agnostic controller responsible for updating the application's active language.
 *
 * This class provides a unified API for changing the application's locale across
 * different platforms. The actual behavior is platform-specific and implemented
 * via `actual` declarations.
 *
 * Typical responsibilities include:
 * - Updating the system or application locale
 * - Triggering locale-dependent behavior (formatting, resources, strings)
 *
 * Note:
 * - Not all platforms support live locale updates.
 * - Some platforms may require application restart or manual UI refresh.
 */
expect class LocalizationController() {

    /**
     * Initializes the controller with platform-specific context if required.
     *
     * On Android, this must be called with the application Context before [update].
     * On other platforms, this is a no-op and can be safely skipped.
     *
     * @param context Platform-specific context (Android Context, or null for other platforms)
     * @return This [LocalizationController] instance for chaining
     */
    fun initialize(context: Any?): LocalizationController

    /**
     * Updates the application's active language.
     *
     * The provided [languageCode] should be an ISO 639-1 language code
     * (e.g. "en", "ro", "fr").
     *
     * Platform-specific behavior:
     * - Android: Updates configuration and resources immediately
     * - Desktop (JVM): Updates the default JVM locale
     * - iOS: Persists language preference and applies it on next app launch
     *
     * @param languageCode ISO 639-1 language code to switch to
     */
    fun update(languageCode: String)
}