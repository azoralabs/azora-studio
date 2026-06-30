package dev.azora.studio.settings

import java.io.File

actual fun azoraLangHasLib(path: String): Boolean =
    path.isNotEmpty() && File(path, "lib").isDirectory

actual fun detectAzoraLangHome(): String {
    val envHome = System.getenv("AZORA_HOME")
    if (envHome != null && File(envHome, "lib").isDirectory) return envHome
    val defaultHome = File(System.getProperty("user.home"), ".azoralang")
    if (defaultHome.isDirectory && File(defaultHome, "lib").isDirectory) return defaultHome.absolutePath
    return ""
}
