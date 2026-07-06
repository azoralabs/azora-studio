package dev.azora.sdk.library.presentation

import dev.azora.sdk.core.project.domain.ProjectRunTarget
import dev.azora.sdk.core.project.domain.ProjectRunTargetKind
import dev.azora.sdk.core.project.domain.ProjectTemplateContribution
import dev.azora.sdk.library.core.InstalledLibrary
import dev.azora.sdk.library.core.LibraryManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipFile

/**
 * Desktop [LibraryManager]: installs and reads library bundles under
 * `~/.azora/libraries/<id>/<version>/library.json`.
 *
 * Bundles are data-only — nothing from a library is loaded into the Studio
 * process. Template contributions are backed by [LibraryTemplateGenerator]
 * (file copy + placeholder substitution) and their run targets always execute
 * as shell commands ([ProjectRunTargetKind.COMMAND]).
 */
class DesktopLibraryManager(
    /** Root of the local library store (overridable for tests). */
    librariesRoot: File = File(System.getProperty("user.home"), ".azora/libraries"),
) : LibraryManager {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val librariesDir: File by lazy { librariesRoot.also { it.mkdirs() } }

    private val _installedLibraries = MutableStateFlow<List<InstalledLibrary>>(emptyList())
    override val installedLibraries: StateFlow<List<InstalledLibrary>> = _installedLibraries.asStateFlow()

    override suspend fun loadInstalledLibraries() = withContext(Dispatchers.IO) {
        val found = mutableListOf<InstalledLibrary>()
        librariesDir.listFiles { f -> f.isDirectory }?.forEach { idDir ->
            // Multiple versions may be installed; expose the newest one.
            val versions = idDir.listFiles { f -> f.isDirectory && File(f, "library.json").isFile }
                ?.sortedWith(compareBy(VersionComparator) { it.name })
                ?: emptyList()
            val newest = versions.lastOrNull() ?: return@forEach
            readManifest(File(newest, "library.json"))?.let { manifest ->
                found += InstalledLibrary(
                    id = manifest.id,
                    name = manifest.name,
                    version = manifest.version,
                    description = manifest.description,
                    type = manifest.type,
                    installPath = newest.absolutePath,
                    templates = manifest.templates,
                )
            }
        }
        _installedLibraries.value = found.sortedBy { it.name }
    }

    override suspend fun installLibrary(sourcePath: String): InstalledLibrary? = withContext(Dispatchers.IO) {
        val source = File(sourcePath)
        val bundleDir: File = when {
            source.isDirectory -> source
            source.isFile && (source.extension.equals("azlib", true) || source.extension.equals("zip", true)) ->
                extractArchive(source) ?: return@withContext null
            else -> return@withContext null
        }

        val manifestFile = findManifest(bundleDir) ?: run {
            println("libraries: no library.json found in $sourcePath")
            return@withContext null
        }
        val manifest = readManifest(manifestFile) ?: return@withContext null
        if (manifest.id.isBlank() || manifest.version.isBlank()) {
            println("libraries: manifest of $sourcePath is missing id/version")
            return@withContext null
        }

        val target = File(librariesDir, "${manifest.id}/${manifest.version}")
        if (target.exists()) target.deleteRecursively()
        target.parentFile.mkdirs()
        manifestFile.parentFile.copyRecursively(target, overwrite = true)
        // Preserve executability of bundle tooling (zip extraction loses it).
        target.walkTopDown()
            .filter { it.isFile && (it.extension == "sh" || it.parentFile.name == "bin") }
            .forEach { it.setExecutable(true) }

        loadInstalledLibraries()
        _installedLibraries.value.find { it.id == manifest.id }
    }

    override suspend fun uninstallLibrary(libraryId: String) = withContext(Dispatchers.IO) {
        val dir = File(librariesDir, libraryId)
        if (dir.isDirectory) dir.deleteRecursively()
        loadInstalledLibraries()
    }

    override fun templateContributions(): List<ProjectTemplateContribution> =
        _installedLibraries.value.flatMap { library ->
            library.templates.map { spec ->
                ProjectTemplateContribution(
                    id = spec.id,
                    label = spec.label,
                    description = spec.description,
                    accentColor = spec.accentColor,
                    generator = LibraryTemplateGenerator(
                        libraryDir = File(library.installPath),
                        templatePath = spec.path,
                        libraryId = library.id,
                        libraryVersion = library.version,
                    ),
                    runTargets = spec.runTargets.map { rt ->
                        ProjectRunTarget(
                            id = rt.id,
                            label = rt.label,
                            kind = ProjectRunTargetKind.COMMAND,
                            command = rt.command,
                            workingDir = rt.workingDir,
                        )
                    },
                )
            }
        }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun readManifest(file: File): LibraryManifest? = try {
        json.decodeFromString<LibraryManifest>(file.readText())
    } catch (e: Exception) {
        println("libraries: failed to read ${file.absolutePath}: ${e.message}")
        null
    }

    /** The bundle's library.json — at the root or exactly one directory down (zip roots). */
    private fun findManifest(dir: File): File? {
        File(dir, "library.json").takeIf { it.isFile }?.let { return it }
        return dir.listFiles { f -> f.isDirectory }
            ?.map { File(it, "library.json") }
            ?.firstOrNull { it.isFile }
    }

    private fun extractArchive(archive: File): File? = try {
        val temp = File.createTempFile("azora_lib_", "").apply { delete(); mkdirs() }
        ZipFile(archive).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val out = File(temp, entry.name)
                // Guard against zip-slip.
                if (!out.canonicalPath.startsWith(temp.canonicalPath + File.separator)) return@forEach
                if (entry.isDirectory) out.mkdirs()
                else {
                    out.parentFile.mkdirs()
                    zip.getInputStream(entry).use { input -> out.outputStream().use { input.copyTo(it) } }
                }
            }
        }
        temp
    } catch (e: Exception) {
        println("libraries: failed to extract ${archive.name}: ${e.message}")
        null
    }

    /** Compares dotted numeric versions ("0.10.2" > "0.9.9"); non-numeric segments compare lexically. */
    private object VersionComparator : Comparator<String> {
        override fun compare(a: String, b: String): Int {
            val pa = a.split('.', '-')
            val pb = b.split('.', '-')
            for (i in 0 until maxOf(pa.size, pb.size)) {
                val sa = pa.getOrNull(i) ?: ""
                val sb = pb.getOrNull(i) ?: ""
                val na = sa.toIntOrNull()
                val nb = sb.toIntOrNull()
                val cmp = if (na != null && nb != null) na.compareTo(nb) else sa.compareTo(sb)
                if (cmp != 0) return cmp
            }
            return 0
        }
    }
}
