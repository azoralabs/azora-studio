package dev.azora.sdk.plugin.presentation.di

import dev.azora.sdk.plugin.presentation.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformPluginModule: Module = module {
    single<PluginManager> { DesktopPluginManager() }
}
