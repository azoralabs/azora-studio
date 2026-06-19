package dev.azora.studio.di

import dev.azora.sdk.core.project.domain.ProjectTemplateGeneratorResolver
import dev.azora.studio.settings.DesktopPluginFilePicker
import dev.azora.studio.settings.PluginFilePicker
import org.koin.dsl.module

/**
 * Desktop-specific DI bindings.
 */
val desktopModule = module {
    // Bridges the data-layer repository (which scaffolds non-Empty templates) to the plugin
    // manager (which knows which templates enabled plugins contribute).
    single<ProjectTemplateGeneratorResolver> { PluginTemplateGeneratorResolver(get()) }

    // Native file picker for installing plugin JARs from Settings.
    single<PluginFilePicker> { DesktopPluginFilePicker() }
}

