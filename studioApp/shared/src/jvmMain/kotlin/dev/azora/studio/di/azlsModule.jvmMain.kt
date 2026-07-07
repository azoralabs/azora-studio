package dev.azora.studio.di

import dev.azora.studio.az_script.AzoraLanguageIntel
import dev.azora.studio.az_script.JarAzoraLanguageIntel
import org.koin.core.module.Module
import org.koin.dsl.module

actual val azlsModule: Module = module {
    single<AzoraLanguageIntel> { JarAzoraLanguageIntel() }
}
