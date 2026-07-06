package dev.azora.sdk.library.presentation.di

import dev.azora.sdk.library.presentation.LibraryManager
import dev.azora.sdk.library.presentation.NoOpLibraryManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformLibraryModule: Module = module {
    single<LibraryManager> { NoOpLibraryManager() }
}
