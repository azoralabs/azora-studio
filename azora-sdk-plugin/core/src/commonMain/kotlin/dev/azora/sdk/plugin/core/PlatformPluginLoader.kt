package dev.azora.sdk.plugin.core

/**
 * Platform-specific plugin loader abstraction.
 * On JVM/Desktop this wraps ClassLoader, on other platforms it's a no-op.
 */
expect class PlatformPluginLoader