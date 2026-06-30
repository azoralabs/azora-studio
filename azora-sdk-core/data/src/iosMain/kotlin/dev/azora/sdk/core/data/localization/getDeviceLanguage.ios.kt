package dev.azora.sdk.core.data.localization

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

/**
 * iOS implementation of [getDeviceLanguage].
 *
 * @return The current device language as an ISO 639-1 code, defaulting to `"en"`.
 */
actual fun getDeviceLanguage(): String =
    NSLocale.currentLocale.languageCode.ifEmpty { "en" }
