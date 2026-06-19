package dev.azora.studio.di

import dev.azora.sdk.core.project.domain.ProjectTemplate
import dev.azora.sdk.core.project.domain.ProjectTemplateGenerator
import dev.azora.sdk.core.project.domain.ProjectTemplateGeneratorResolver
import dev.azora.sdk.plugin.presentation.PluginManager

/**
 * Resolves a [ProjectTemplate] to the [ProjectTemplateGenerator] contributed by an installed,
 * enabled plugin. With no plugins (or none contributing the template) this returns `null`, so the
 * only scaffoldable template is [ProjectTemplate.EMPTY].
 */
class PluginTemplateGeneratorResolver(
    private val pluginManager: PluginManager
) : ProjectTemplateGeneratorResolver {

    override suspend fun generatorFor(template: ProjectTemplate): ProjectTemplateGenerator? =
        pluginManager.templateContributions().firstOrNull { it.id == template.name }?.generator
}
