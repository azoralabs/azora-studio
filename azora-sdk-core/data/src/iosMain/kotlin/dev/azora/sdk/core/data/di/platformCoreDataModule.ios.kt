package dev.azora.sdk.core.data.di

import dev.azora.sdk.core.data.auth.DataStoreSessionStorage
import dev.azora.sdk.core.data.auth.createDataStore
import dev.azora.sdk.core.data.localization.LocalizationServiceImpl
import dev.azora.sdk.core.data.localization.getDeviceLanguage
import dev.azora.sdk.core.data.preferences.DataStoreThemePreferences
import dev.azora.sdk.core.domain.auth.SessionStorage
import dev.azora.sdk.core.domain.localization.LocalizationController
import dev.azora.sdk.core.domain.localization.LocalizationService
import dev.azora.sdk.core.domain.preferences.ThemePreferences
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * iOS dependency injection module for the core data layer.
 *
 * Backs networking with the Ktor Darwin engine and persists preferences via a Documents-directory
 * [androidx.datastore.core.DataStore].
 */
actual val platformCoreDataModule = module {

    single { createDataStore() }

    single<HttpClientEngine> { Darwin.create() }

    singleOf(::DataStoreSessionStorage) bind SessionStorage::class
    singleOf(::DataStoreThemePreferences) bind ThemePreferences::class

    single {
        LocalizationServiceImpl(
            sessionStorage = get(),
            deviceLanguageProvider = ::getDeviceLanguage,
            updateSystemLocale = { languageCode ->
                LocalizationController().update(languageCode)
            }
        )
    } bind LocalizationService::class
}
