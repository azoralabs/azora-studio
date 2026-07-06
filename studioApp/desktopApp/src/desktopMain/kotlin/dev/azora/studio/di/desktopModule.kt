package dev.azora.studio.di

import dev.azora.sdk.core.project.domain.ProjectTemplateGeneratorResolver
import dev.azora.studio.settings.DesktopLibraryFilePicker
import dev.azora.studio.settings.DesktopPluginFilePicker
import dev.azora.studio.settings.LibraryFilePicker
import dev.azora.studio.settings.PluginFilePicker
import org.koin.dsl.module

/**
 * Desktop-specific DI bindings.
 */
val desktopModule = module {
    // Bridges the data-layer repository (which scaffolds non-Empty templates) to the plugin
    // manager and the library manager (both of which contribute templates).
    single<ProjectTemplateGeneratorResolver> { PluginTemplateGeneratorResolver(get(), get()) }

    // Native file picker for installing plugin JARs from Settings.
    single<PluginFilePicker> { DesktopPluginFilePicker() }

    // Native picker for installing library bundles (folders or .azlib archives).
    single<LibraryFilePicker> { DesktopLibraryFilePicker() }
}

