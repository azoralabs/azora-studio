package dev.azora.sdk.core.presentation.di

import dev.azora.sdk.core.presentation.screen.ScreenViewModel
import dev.azora.sdk.core.presentation.undoredo.GlobalUndoRedoCoordinator
import dev.azora.sdk.core.presentation.util.ScopedStoreRegistryViewModel
import dev.azora.canvas.domain.interpreter.ConsoleOutputManager
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Provides presentation-layer dependencies for the application using Koin.
 *
 * This module is responsible for exposing UI-related ViewModels and any
 * presentation-scoped services. It centralizes all dependencies that are
 * directly consumed by Composables or UI controllers.
 *
 * Currently, includes:
 *
 * - [ScopedStoreRegistryViewModel] — manages scoped ViewModel stores used
 *   for dialogs, sheets, nested navigation flows, and other temporary UI
 *   components that require isolated state.
 *
 * - [ScreenViewModel] — holds screen-level UI state (e.g., dialog visibility)
 *   and exposes actions for updating that state. Declared as a `single` to
 *   ensure the same instance is shared across the app’s composable hierarchy.
 *
 * Extend this module as the application grows to include additional
 * presentation-layer singletons, state holders, or ViewModels required
 * by your UI.
 */
val corePresentationModule = module {
    viewModelOf(::ScopedStoreRegistryViewModel)
    single { ScreenViewModel() }
    single { GlobalUndoRedoCoordinator() }
    single { ConsoleOutputManager() }
}