package dev.azora.sdk.core.data.di

import dev.azora.sdk.core.data.logging.KermitLogger
import dev.azora.sdk.core.data.networking.HttpClientFactory
import dev.azora.sdk.core.data.notification.KtorDeviceTokenService
import dev.azora.sdk.core.domain.logging.AzoraLogger
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.*

/**
 * Platform-specific dependency injection module for the core data layer.
 *
 * Each target (Android, iOS, Desktop, JVM) provides its own `actual` implementation
 * that registers platform-specific dependencies such as:
 * - [androidx.datastore.core.DataStore] creation logic
 * - [io.ktor.client.engine.HttpClientEngine] setup
 */
expect val platformCoreDataModule: Module

/**
 * Common dependency injection module for the core data layer.
 *
 * This module aggregates shared and platform-specific dependencies required by
 * the data layer. It is intended to be included in the application's root
 * dependency graph.
 *
 * Included modules:
 * - [dev.azora.sdk.core.data.di.platformCoreDataModule] – provides platform-specific implementations
 *
 * Provided bindings:
 * - [dev.azora.sdk.core.domain.logging.AzoraLogger] → [dev.azora.sdk.core.data.logging.KermitLogger]
 *   Shared logging implementation used across all data-layer services.
 *
 * - [HttpClient]
 *   Main Ktor HTTP client configured via [dev.azora.sdk.core.data.networking.HttpClientFactory], used for all
 *   network communication.
 *
 * - [AuthService] → [KtorAuthService]
 *   Handles authentication-related API interactions.
 *
 * - [SessionStorage] → [DataStoreSessionStorage]
 *   Persists session-related data (e.g. tokens, user state).
 *
 * - [DeviceTokenService] -> [dev.azora.sdk.core.data.notification.KtorDeviceTokenService]
 *   Handles device token-related API interactions.
 *
 * Notes:
 * - All dependencies are registered as singletons.
 * - This module is platform-agnostic and relies on
 *   [dev.azora.sdk.core.data.di.platformCoreDataModule] for platform-specific concerns.
 */
val coreDataModule = module {

    includes(platformCoreDataModule)

    single<AzoraLogger> { KermitLogger }

    single {
        HttpClientFactory(get(), get()).create(get())
    }

    single<HttpClient> {
        HttpClientFactory(get(), get()).create(get())
    }

    //singleOf(::KtorAuthService) bind AuthService::class
    // SessionStorage is provided by platformCoreDataModule
    // (DataStore on Android/iOS/Desktop, localStorage on Web)
    //singleOf(::KtorDeviceTokenService) bind DeviceTokenService::class
}