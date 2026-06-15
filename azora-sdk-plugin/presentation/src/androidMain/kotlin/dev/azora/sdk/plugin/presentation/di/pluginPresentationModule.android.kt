package dev.azora.sdk.plugin.presentation.di

import dev.azora.sdk.plugin.presentation.NoOpPluginManager
import dev.azora.sdk.plugin.presentation.PluginManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformPluginModule: Module = module {
    single<PluginManager> { NoOpPluginManager() }
}
