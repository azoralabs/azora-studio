package dev.azora.sdk.core.project.data.di

import dev.azora.sdk.core.project.data.repository.LocalAzoraProjectRepository
import dev.azora.sdk.core.project.data.repository.LocalSettingsRepository
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.sdk.core.project.domain.repository.SettingsRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.*

actual val platformAzoraProjectDataModule = module {
    singleOf(::LocalAzoraProjectRepository) bind AzoraProjectRepository::class
    singleOf(::LocalSettingsRepository) bind SettingsRepository::class
}