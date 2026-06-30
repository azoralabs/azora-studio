package dev.azora.studio.settings

// Mobile has no local azora-lang toolchain.
actual fun azoraLangHasLib(path: String): Boolean = false

actual fun detectAzoraLangHome(): String = ""
