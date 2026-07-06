package dev.azora.studio.library

import dev.azora.sdk.library.presentation.LibraryManager
import java.io.File

/**
 * Installs the library bundles shipped inside the Studio itself (currently the
 * Azora Engine) into `~/.azora/libraries` on startup.
 *
 * Bundles are embedded as app resources under `bundled-libraries/` (see the
 * `embedBundledLibraries` Gradle task) with an `index.txt` listing the
 * `<id>-<version>.azlib` files. A bundle is extracted only when that exact
 * id/version is not installed yet, so user installs and upgrades are never
 * overwritten and subsequent launches are effectively free.
 */
object BundledLibraries {

    suspend fun installIfMissing(manager: LibraryManager) {
        val index = BundledLibraries::class.java.getResourceAsStream("/bundled-libraries/index.txt")
            ?: return
        val entries = index.bufferedReader().use { it.readLines() }
            .map { it.trim() }
            .filter { it.endsWith(".azlib") }

        var installedAny = false
        for (fileName in entries) {
            val base = fileName.removeSuffix(".azlib")
            val version = base.substringAfterLast('-')
            val id = base.substringBeforeLast('-')
            if (id.isBlank() || version.isBlank()) continue

            val target = File(System.getProperty("user.home"), ".azora/libraries/$id/$version")
            if (target.isDirectory) continue

            val resource = BundledLibraries::class.java.getResourceAsStream("/bundled-libraries/$fileName")
                ?: continue
            val temp = File.createTempFile("azora_bundled_", ".azlib")
            try {
                resource.use { input -> temp.outputStream().use { input.copyTo(it) } }
                if (manager.installLibrary(temp.absolutePath) != null) {
                    installedAny = true
                    println("libraries: installed bundled $id $version")
                }
            } finally {
                temp.delete()
            }
        }

        if (installedAny) {
            manager.loadInstalledLibraries()
        }
    }
}
