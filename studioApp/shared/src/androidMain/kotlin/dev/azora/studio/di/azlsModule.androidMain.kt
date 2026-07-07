package dev.azora.studio.di

import dev.azora.studio.az_script.AzoraLanguageIntel
import dev.azora.studio.az_script.AzoraScriptDebugger
import dev.azora.studio.az_script.NoOpAzoraLanguageIntel
import dev.azora.studio.az_script.NoOpAzoraScriptDebugger
import org.koin.core.module.Module
import org.koin.dsl.module

actual val azlsModule: Module = module {
    single<AzoraLanguageIntel> { NoOpAzoraLanguageIntel }
    single<AzoraScriptDebugger> { NoOpAzoraScriptDebugger }
}
