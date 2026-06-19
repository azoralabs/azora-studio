package dev.azora.studio.di

import dev.azora.sdk.core.project.domain.ProjectTemplateGenerator
import dev.azora.sdk.core.project.domain.ProjectTemplateGeneratorResolver
import dev.azora.sdk.plugin.presentation.PluginManager

/**
 * Resolves a template id to the [ProjectTemplateGenerator] contributed by an installed, enabled
 * plugin. With no plugin contributing the template this returns `null` (the builtin "empty" template
 * is handled directly by the data-layer repository, not here).
 */
class PluginTemplateGeneratorResolver(
    private val pluginManager: PluginManager
) : ProjectTemplateGeneratorResolver {

    override suspend fun generatorFor(templateId: String): ProjectTemplateGenerator? =
        pluginManager.templateContributions().firstOrNull { it.id == templateId }?.generator
}
