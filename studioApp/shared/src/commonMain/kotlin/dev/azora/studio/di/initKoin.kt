package dev.azora.studio.di

import dev.azora.sdk.core.data.di.coreDataModule
import dev.azora.sdk.core.data.logging.KermitLogger
import dev.azora.sdk.core.domain.logging.AzoraLogger
import dev.azora.sdk.core.io.di.coreIoModule
import dev.azora.sdk.core.presentation.di.corePresentationModule
import dev.azora.sdk.core.project.data.di.azoraProjectDataModule
import dev.azora.sdk.library.presentation.di.libraryPresentationModule
import dev.azora.sdk.plugin.presentation.di.pluginPresentationModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Initializes the Koin dependency injection framework with all required modules.
 *
 * This function should be called once during application startup to configure
 * the DI container with all necessary dependencies.
 *
 * @param config Optional configuration block for additional Koin setup
 *               (e.g., enabling logging, adding more modules)
 */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            appModule,
            initCache,
            azlsModule,

            loggingModule,
            coreDataModule,
            coreIoModule,
            corePresentationModule,
            azoraProjectDataModule,
            pluginPresentationModule,
            libraryPresentationModule,
        )
    }
}

private val loggingModule = module {
    single<AzoraLogger> { KermitLogger }
}

expect val initCache: Module

/** Binds [dev.azora.studio.az_script.AzoraLanguageIntel] — the azls-jar bridge on JVM, a no-op elsewhere. */
expect val azlsModule: Module