package dev.azora.studio.di

import dev.azora.sdk.core.project.domain.ProjectTemplateGenerator
import dev.azora.sdk.core.project.domain.ProjectTemplateGeneratorResolver
import dev.azora.sdk.library.presentation.LibraryManager
import dev.azora.sdk.plugin.presentation.PluginManager

/**
 * Resolves a template id to the [ProjectTemplateGenerator] contributed by an installed, enabled
 * plugin or by an installed library (e.g. the Azora Engine's "App"/"Game" templates). With no
 * contributor this returns `null` (the builtin "empty" template is handled directly by the
 * data-layer repository, not here).
 */
class PluginTemplateGeneratorResolver(
    private val pluginManager: PluginManager,
    private val libraryManager: LibraryManager
) : ProjectTemplateGeneratorResolver {

    override suspend fun generatorFor(templateId: String): ProjectTemplateGenerator? =
        pluginManager.templateContributions().firstOrNull { it.id == templateId }?.generator
            ?: libraryManager.templateContributions().firstOrNull { it.id == templateId }?.generator
}
