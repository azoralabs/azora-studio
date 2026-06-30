package dev.azora.sdk.core.data.util

/**
 * Android implementation of [PlatformUtils].
 */
actual object PlatformUtils {

    /**
     * @return The fixed OS name `"Android"`.
     */
    actual fun getOSName(): String = "Android"
}
