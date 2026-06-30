package dev.azora.sdk.core.data.localization

import java.util.Locale

/**
 * JVM implementation of [getDeviceLanguage].
 *
 * Returns the language code of the system default [Locale].
 *
 * @return The device language code as an ISO 639-1 string.
 */
actual fun getDeviceLanguage(): String = Locale.getDefault().language