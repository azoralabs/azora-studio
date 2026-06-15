package dev.azora.sdk.core.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.scene.Scene

/**
 * Utility object providing navigation configuration helpers.
 *
 * This object contains common navigation setup functions used throughout the app,
 * including state preservation decorators and transition configurations.
 *
 * ## Features
 * - **State Preservation**: Decorators for saving state and ViewModel across navigation
 * - **Transition Control**: Helpers for customizing screen transitions
 *
 * ## Usage
 * ```kotlin
 * NavSceneHost(
 *     entries = entries,
 *     transitionSpec = Navigation.noTransition()
 * )
 * ```
 *
 * @see NavigationState For managing navigation state
 * @see Navigator For performing navigation actions
 */
object Navigation {

    /**
     * Creates a list of [NavEntryDecorator]s that enable state preservation for navigation entries.
     *
     * This composable function returns decorators that:
     * 1. **SaveableStateHolder**: Preserves UI state (scroll position, text input, etc.)
     *    across configuration changes and process death
     * 2. **ViewModelStore**: Preserves ViewModels scoped to navigation entries,
     *    surviving configuration changes
     *
     * ## Usage
     * ```kotlin
     * val decorators = Navigation.saveableViewModelState<Route>()
     * rememberDecoratedNavEntries(
     *     backStack = backStack,
     *     entryDecorators = decorators,
     *     entryProvider = { route -> ... }
     * )
     * ```
     *
     * @param T The type of navigation key used (typically [Route])
     * @return List of decorators for state and ViewModel preservation
     */
    @Composable
    fun <T : Any> saveableViewModelState(): List<NavEntryDecorator<T>>  = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
    )

    /**
     * Creates a transition specification that disables all screen transitions.
     *
     * Returns a lambda that produces a [ContentTransform] with no enter or exit
     * animations. Useful for instant screen switches where animations would be
     * distracting or inappropriate (e.g., tab switching in bottom navigation).
     *
     * ## Usage
     * ```kotlin
     * NavSceneHost(
     *     entries = entries,
     *     transitionSpec = Navigation.noTransition()
     * )
     * ```
     *
     * @param T The type of navigation key used
     * @return A transition spec that produces no animation
     */
    fun <T : Any> noTransition():
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.None
        )
    }
}