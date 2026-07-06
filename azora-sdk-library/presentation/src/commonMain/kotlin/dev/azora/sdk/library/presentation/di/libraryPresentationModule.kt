package dev.azora.sdk.library.presentation.di

import dev.azora.sdk.library.presentation.LibraryManagerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformLibraryModule: Module

val libraryPresentationModule = module {
    includes(platformLibraryModule)
    viewModelOf(::LibraryManagerViewModel)
}
