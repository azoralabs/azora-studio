package dev.azora.sdk.library.presentation

import dev.azora.sdk.core.io.ExistsResult
import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.ProjectTemplateGenerator
import dev.azora.sdk.library.core.LibraryTemplatePlaceholders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Generates a project from a library template: copies the template file tree
 * into the new project directory, substituting `__AZORA_*__` placeholders
 * ([LibraryTemplatePlaceholders]) in every file. Shell scripts are marked
 * executable so run targets (`sh run.sh`) work immediately.
 *
 * The template source is an absolute location inside the installed bundle;
 * writes go through the host [FileSystem] so project paths resolve exactly
 * like every other generator.
 */
class LibraryTemplateGenerator(
    private val libraryDir: File,
    private val templatePath: String,
    private val libraryId: String,
    private val libraryVersion: String,
) : ProjectTemplateGenerator {

    override suspend fun generate(
        project: AzoraProjectModel,
        projectPath: String,
        fileSystem: FileSystem,
    ) = withContext(Dispatchers.IO) {
        val templateDir = File(libraryDir, templatePath)
        if (!templateDir.isDirectory) {
            println("libraries: template dir missing: ${templateDir.absolutePath}")
            return@withContext
        }

        val substitutions = mapOf(
            LibraryTemplatePlaceholders.PROJECT_NAME to project.name,
            LibraryTemplatePlaceholders.PACKAGE_NAME to project.packageName,
            LibraryTemplatePlaceholders.LIBRARY_ID to libraryId,
            LibraryTemplatePlaceholders.LIBRARY_VERSION to libraryVersion,
        )

        templateDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(templateDir).invariantSeparatorsPath
            val target = "$projectPath/$relative"

            // The host re-runs generators before each Run (idempotent scaffolding).
            // Library templates seed real source files the user edits, so never
            // overwrite an existing file — only restore missing ones.
            if (fileSystem.fileExists(target) is ExistsResult.Exists) return@forEach

            var content = file.readText()
            for ((placeholder, value) in substitutions) {
                content = content.replace(placeholder, value)
            }

            fileSystem.writeToFile(target, content)
            if (file.extension == "sh") {
                fileSystem.setExecutable(target)
            }
        }
    }
}
