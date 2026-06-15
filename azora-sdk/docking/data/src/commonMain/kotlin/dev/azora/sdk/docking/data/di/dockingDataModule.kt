package dev.azora.sdk.docking.data.di

import dev.azora.sdk.docking.data.DockStateManagerImpl
import dev.azora.sdk.docking.domain.DockStateManager
import org.koin.dsl.module

/**
 * Koin dependency injection module for the docking data layer.
 *
 * This module provides the default implementations for the docking SDK's
 * data layer dependencies. Include this module in your Koin configuration
 * to enable docking functionality.
 *
 * ## Usage
 *
 * Include in your Koin application modules:
 *
 * ```kotlin
 * startKoin {
 *     modules(
 *         dockingDataModule,
 *         // ... other modules
 *     )
 * }
 * ```
 *
 * Then inject [DockStateManager] where needed:
 *
 * ```kotlin
 * class MyViewModel(
 *     private val dockStateManager: DockStateManager
 * ) : ViewModel() {
 *     // Use dockStateManager to control the dock
 * }
 * ```
 *
 * ## Provided Dependencies
 *
 * | Interface | Implementation | Scope |
 * |-----------|----------------|-------|
 * | [DockStateManager] | [DockStateManagerImpl] | Singleton |
 *
 * ## Custom Configuration
 *
 * To provide a custom initial layout, override the binding:
 *
 * ```kotlin
 * modules(
 *     dockingDataModule,
 *     module {
 *         single<DockStateManager>(override = true) {
 *             DockStateManagerImpl(initialLayout = myCustomLayout)
 *         }
 *     }
 * )
 * ```
 *
 * @see DockStateManager for the interface contract
 * @see DockStateManagerImpl for the implementation
 */
val dockingDataModule = module {

    /**
     * Provides a singleton [DockStateManager] instance.
     *
     * The state manager is created with an empty initial layout. Override
     * this binding to provide a custom initial layout.
     */
    single<DockStateManager> { DockStateManagerImpl() }
}