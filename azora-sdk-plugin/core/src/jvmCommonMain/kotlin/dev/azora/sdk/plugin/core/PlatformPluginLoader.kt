package dev.azora.sdk.plugin.core

/**
 * JVM implementation wrapping ClassLoader for dynamic plugin loading.
 * Shared between Android and Desktop.
 */
actual class PlatformPluginLoader(
    val classLoader: ClassLoader
)