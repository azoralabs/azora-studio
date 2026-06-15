package dev.azora.sdk.core.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*

/**
 * Converts the [NavigationState] into a list of [NavEntry] objects for rendering.
 *
 * This extension function transforms the navigation back stacks into decorated navigation entries.
 * It applies state preservation decorators to each entry.
 *
 * ## Decorators Applied
 * Each navigation entry is wrapped with:
 * 1. **SaveableStateHolder**: Preserves UI state across configuration changes
 * 2. **ViewModelStore**: Preserves ViewModels scoped to each navigation entry
 *
 * ## Entry Selection
 * Only entries from [NavigationState.stacksInUse] are included:
 * - At start route: only start route's back stack entries
 * - At other tabs: start route + current tab's back stack entries
 *
 * This ensures proper state preservation while avoiding unnecessary composition
 * of inactive tab contents.
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun AppNavHost(navigationState: NavigationState) {
 *     val entries = navigationState.toEntries { route ->
 *         NavEntry(route) {
 *             when (route) {
 *                 is Route.Menu.Map.ViewMap -> MapScreen()
 *                 is Route.Menu.Map.ViewIncident -> IncidentScreen(route.incidentId)
 *                 // ... handle all routes
 *             }
 *         }
 *     }
 *
 *     NavSceneHost(
 *         entries = entries,
 *         transitionSpec = Navigation.noTransition()
 *     )
 * }
 * ```
 *
 * @param entryProvider Lambda that creates a [NavEntry] for each route key.
 *                      This defines what composable content to render for each route.
 * @return A [SnapshotStateList] of decorated [NavEntry] objects ready for rendering
 *
 * @see NavigationState The state this extension operates on
 * @see Navigation.saveableViewModelState For the decorators applied to entries
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator()
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider
        )
    }

    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}