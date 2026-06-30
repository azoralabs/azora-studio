package dev.azora.studio.settings

/**
 * Whether [path] looks like a valid azora-lang install (contains a `lib` directory).
 *
 * Desktop-only concern; mobile platforms have no local toolchain and always return false.
 */
expect fun azoraLangHasLib(path: String): Boolean

/**
 * Best-effort detection of a local azora-lang home directory, or an empty string if none.
 */
expect fun detectAzoraLangHome(): String
