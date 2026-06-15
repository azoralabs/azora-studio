package dev.azora.sdk.core.data.util

/**
 * Desktop-specific implementation of [dev.azora.sdk.core.data.util.PlatformUtils].
 *
 * This implementation uses the JVM system property `"os.name"`
 * to determine the current operating system name at runtime.
 *
 * Common examples of returned values include:
 * - `"Windows 11"`
 * - `"Linux"`
 * - `"Mac OS X"`
 *
 * This is primarily used for logging, analytics, or platform-specific
 * behavior when running on JVM-based desktop environments.
 */
actual object PlatformUtils {

    /**
     * Returns the name of the current operating system as reported
     * by the JVM system property `"os.name"`.
     *
     * @return A string describing the operating system (e.g., `"Windows 11"`, `"Linux"`).
     */
    actual fun getOSName(): String = System.getProperty("os.name")
}