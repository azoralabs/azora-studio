package dev.azora.sdk.library.presentation

import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.ProjectRunTargetKind
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Installs a library bundle and generates a project from one of its templates,
 * exercising the same path the Studio uses (Libraries → install; New Project →
 * Game template).
 *
 * Uses a synthetic minimal bundle so the test is self-contained; when the real
 * Azora Engine dist bundle is present next to the repository, it is exercised
 * too.
 */
class DesktopLibraryManagerTest {

    private fun tempDir(prefix: String): File =
        File.createTempFile(prefix, "").apply { delete(); mkdirs() }

    private fun writeMinimalBundle(dir: File) {
        File(dir, "library.json").writeText(
            """
            {
              "id": "test-lib",
              "name": "Test Library",
              "version": "1.2.3",
              "description": "test",
              "templates": [
                {
                  "id": "test-app",
                  "label": "App",
                  "description": "test app",
                  "path": "templates/app",
                  "runTargets": [ { "id": "run", "label": "Run", "command": "sh run.sh" } ]
                }
              ]
            }
            """.trimIndent()
        )
        File(dir, "templates/app/src").mkdirs()
        File(dir, "templates/app/src/main.az").writeText(
            "// __AZORA_PROJECT_NAME__ by __AZORA_LIBRARY_ID__@__AZORA_LIBRARY_VERSION__\n"
        )
        File(dir, "templates/app/run.sh").writeText("#!/bin/sh\necho __AZORA_PROJECT_NAME__\n")
    }

    @Test
    fun installsBundleContributesTemplatesAndGeneratesProject() = runBlocking {
        val root = tempDir("azlib_root_")
        val bundle = tempDir("azlib_bundle_")
        writeMinimalBundle(bundle)

        val manager = DesktopLibraryManager(librariesRoot = root)
        val installed = manager.installLibrary(bundle.absolutePath)
        assertNotNull(installed, "bundle should install")
        assertEquals("test-lib", installed.id)
        assertEquals("1.2.3", installed.version)
        assertTrue(File(root, "test-lib/1.2.3/library.json").isFile)

        // Template contribution with COMMAND run target.
        val contributions = manager.templateContributions()
        assertEquals(1, contributions.size)
        val template = contributions.first()
        assertEquals("test-app", template.id)
        assertEquals(ProjectRunTargetKind.COMMAND, template.runTargets.single().kind)
        assertEquals("sh run.sh", template.runTargets.single().command)

        // Generate a project from the template (absolute path → FileSystem passes it through).
        val projectDir = tempDir("azlib_project_")
        val project = AzoraProjectModel(
            id = "p1", name = "MyGame", companyName = "Acme",
            packageName = "com.acme.mygame", version = "1.0", engineVersion = "1",
            template = "test-app",
        )
        template.generator.generate(project, projectDir.absolutePath, FileSystem())

        val main = File(projectDir, "src/main.az").readText()
        assertEquals("// MyGame by test-lib@1.2.3\n", main)
        assertTrue(File(projectDir, "run.sh").canExecute(), "run.sh should be executable")

        // Regeneration must not clobber user edits.
        File(projectDir, "src/main.az").writeText("edited")
        template.generator.generate(project, projectDir.absolutePath, FileSystem())
        assertEquals("edited", File(projectDir, "src/main.az").readText())

        // Uninstall removes the library.
        manager.uninstallLibrary("test-lib")
        assertTrue(manager.installedLibraries.value.isEmpty())
    }

    @Test
    fun installsRealEngineBundleWhenPresent() = runBlocking {
        // azora-studio/azora-sdk-library/presentation → ../../../azora-engine
        val dist = File("../../../azora-engine/dist").canonicalFile
        val bundle = dist.listFiles { f -> f.isDirectory && File(f, "library.json").isFile }
            ?.firstOrNull() ?: return@runBlocking // skip when the engine dist isn't built

        val root = tempDir("azlib_engine_root_")
        val manager = DesktopLibraryManager(librariesRoot = root)
        val installed = manager.installLibrary(bundle.absolutePath)
        assertNotNull(installed, "engine bundle should install")
        assertEquals("azora-engine", installed.id)

        val templates = manager.templateContributions()
        assertEquals(setOf("azora-engine-app", "azora-engine-game"), templates.map { it.id }.toSet())
        assertTrue(templates.all { it.runTargets.isNotEmpty() })
    }
}
