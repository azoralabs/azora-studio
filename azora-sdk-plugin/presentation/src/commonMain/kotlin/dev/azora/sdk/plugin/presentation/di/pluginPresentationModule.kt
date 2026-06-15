package dev.azora.sdk.plugin.presentation.di

import dev.azora.sdk.plugin.presentation.PluginManagerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformPluginModule: Module

val pluginPresentationModule = module {
    includes(platformPluginModule)
    viewModelOf(::PluginManagerViewModel)
}
