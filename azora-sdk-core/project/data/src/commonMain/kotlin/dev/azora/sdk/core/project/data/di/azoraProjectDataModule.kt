package dev.azora.sdk.core.project.data.di

import dev.azora.sdk.core.project.data.repository.LocalAzoraProjectRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform-specific module for ProjectRepository binding.
 * - nonWebMain: Uses [LocalAzoraProjectRepository] with LocalDatabase
 */
expect val platformAzoraProjectDataModule: Module

val azoraProjectDataModule = module {
    includes(platformAzoraProjectDataModule)
}