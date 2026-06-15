package dev.azora.sdk.core.data.di

import dev.azora.sdk.core.data.auth.*
import dev.azora.sdk.core.data.localization.*
import dev.azora.sdk.core.data.preferences.DataStoreThemePreferences
import dev.azora.sdk.core.domain.auth.SessionStorage
import dev.azora.sdk.core.domain.localization.*
import dev.azora.sdk.core.domain.preferences.ThemePreferences
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.*

/**
 * Desktop (JVM) dependency injection module for the core data layer.
 *
 * This module provides platform-dependent implementations required by
 * the shared core data layer and is included by [dev.azora.sdk.core.data.di.coreDataModule].
 *
 * Provided bindings:
 * - [HttpClientEngine] → OkHttp
 *   Ktor HTTP engine backed by OkHttp for JVM networking.
 *
 * - [androidx.datastore.core.DataStore]<[androidx.datastore.preferences.core.Preferences]>
 *   JVM-backed preferences storage for persisting user settings.
 *
 * - [ThemePreferences] → [DataStoreThemePreferences]
 *   Stores and manages theme-related preferences on desktop.
 *
 * - [LocalizationService] → [LocalizationServiceImpl]
 *   Desktop localization service that updates the JVM default locale.
 *
 * Notes:
 * - All dependencies are registered as singletons.
 * - Updating the locale affects JVM locale-sensitive APIs
 *   (e.g. date and number formatting).
 * - UI frameworks do not automatically recompose; manual refresh may be required.
 */
actual val platformCoreDataModule = module {

    single {
        createDataStore()
    }

    single<HttpClientEngine> { OkHttp.create() }

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