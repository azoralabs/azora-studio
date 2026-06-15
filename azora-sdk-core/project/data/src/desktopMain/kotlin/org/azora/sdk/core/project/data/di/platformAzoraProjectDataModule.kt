package dev.azora.sdk.core.project.data.di

import dev.azora.sdk.core.project.data.repository.LocalAzoraProjectRepository
import dev.azora.sdk.core.project.data.repository.LocalSettingsRepository
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.sdk.core.project.domain.repository.SettingsRepository
import dev.koin.core.module.dsl.singleOf
import dev.koin.dsl.*

actual val platformAzoraProjectDataModule = module {
    singleOf(::LocalAzoraProjectRepository) bind AzoraProjectRepository::class
    singleOf(::LocalSettingsRepository) bind SettingsRepository::class
}