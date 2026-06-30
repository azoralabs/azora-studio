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
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Android dependency injection module for the core data layer.
 *
 * Requires the application to register the Android context via `androidContext(...)` during
 * Koin start-up so the [androidx.datastore.core.DataStore] and [LocalizationController] can
 * resolve the application [android.content.Context].
 */
actual val platformCoreDataModule = module {

    single { createDataStore(androidContext()) }

    single<HttpClientEngine> { OkHttp.create() }

    singleOf(::DataStoreSessionStorage) bind SessionStorage::class
    singleOf(::DataStoreThemePreferences) bind ThemePreferences::class

    single {
        LocalizationServiceImpl(
            sessionStorage = get(),
            deviceLanguageProvider = ::getDeviceLanguage,
            updateSystemLocale = { languageCode ->
                LocalizationController().initialize(androidContext()).update(languageCode)
            }
        )
    } bind LocalizationService::class
}
