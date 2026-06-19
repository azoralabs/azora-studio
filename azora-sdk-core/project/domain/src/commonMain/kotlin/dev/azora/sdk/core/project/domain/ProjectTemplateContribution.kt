package dev.azora.sdk.core.project.domain

import dev.azora.sdk.core.io.FileSystem

/**
 * Generates the source/build files for one [ProjectTemplate] when a project of that template is
 * created (or regenerated).
 *
 * Implementations live in plugins (e.g. the Website builder ships a Kobweb generator). The host
 * (Azora Studio) never references generator implementations directly — it resolves them through a
 * [ProjectTemplateGeneratorResolver] backed by the plugin manager.
 */
interface ProjectTemplateGenerator {

    /**
     * Scaffold the template into [projectPath] using [fileSystem].
     *
     * @param project The project being created (template-specific defaults already seeded).
     * @param projectPath Absolute/relative root directory of the project.
     * @param fileSystem The host file system used to write files.
     */
    suspend fun generate(project: AzoraProjectModel, projectPath: String, fileSystem: FileSystem)
}

/**
 * A project template contributed by a plugin.
 *
 * @property id Stable identifier matching a [ProjectTemplate] enum entry name (e.g. `"WEBSITE"`).
 *   This keeps the serialized project model and DB untouched while letting plugins surface the
 *   template in the create-project UI and own its generation.
 * @property label Human-readable name shown in the template picker.
 * @property description Short explanation of what the template scaffolds.
 * @property accentColor Hex color used for the template card accent.
 * @property iconXml Optional inline vector icon XML rendered on the card.
 * @property generator The [ProjectTemplateGenerator] that scaffolds the project.
 */
data class ProjectTemplateContribution(
    val id: String,
    val label: String,
    val description: String,
    val accentColor: String = "#FF9C27B0",
    val iconXml: String? = null,
    val generator: ProjectTemplateGenerator
)

/**
 * Resolves the [ProjectTemplateGenerator] for a given [ProjectTemplate], or `null` if no installed
 * plugin contributes it.
 *
 * Defined in the domain layer so the data-layer repository can depend on it without coupling to the
 * plugin SDK. The host provides the implementation (backed by the plugin manager).
 */
fun interface ProjectTemplateGeneratorResolver {
    suspend fun generatorFor(template: ProjectTemplate): ProjectTemplateGenerator?
}
