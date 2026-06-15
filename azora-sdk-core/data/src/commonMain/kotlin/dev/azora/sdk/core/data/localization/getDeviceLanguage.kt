package dev.azora.sdk.core.data.localization

/**
 * Returns the current device language as an ISO 639-1 code.
 *
 * Platform-specific implementations provide the actual language code
 * according to the device settings.
 *
 * @return The device language code (e.g., `"en"` for English, `"ro"` for Romanian)
 */
expect fun getDeviceLanguage(): String