package dev.azora.sdk.core.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation3.runtime.*

/**
 * Holds the navigation state for the application, supporting hierarchical navigation
 * with separate back stacks for each top-level destination.
 *
 * This class manages a multi-stack navigation pattern commonly used with bottom navigation
 * or tab-based UIs, where each tab maintains its own independent back stack.
 *
 * ## Architecture
 * ```
 * NavigationState
 * ├── startRoute (e.g., Route.Menu.Map)
 * ├── topLevelRoute (currently active tab)
 * └── backStacks (Map)
 *     ├── Route.Menu.Map → [Map, ViewIncident("123")]
 *     ├── Route.Menu.Incidents → [Incidents, SelectIncident]
 *     └── Route.Menu.Account → [Account, MyProfile]
 * ```
 *
 * ## Usage
 * Created via [rememberNavigationState] and used with [Navigator] for navigation:
 * ```kotlin
 * val navigationState = rememberNavigationState(
 *     startRoute = Route.Menu.Map,
 *     topLevelRoutes = setOf(Route.Menu.Map, Route.Menu.Incidents, Route.Menu.Account),
 *     serializersConfig = serializersConfig
 * )
 * val navigator = Navigator(navigationState)
 * ```
 *
 * @property startRoute The initial/default top-level route (typically the first tab)
 * @property topLevelRoute The currently active top-level route (observable via delegation)
 * @property backStacks Map of each top-level route to its navigation back stack
 *
 * @see rememberNavigationState Factory function to create NavigationState
 * @see Navigator Controller that manipulates this state
 * @see toEntries Extension to convert state to NavEntry list for rendering
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {

    /**
     * The currently active top-level route.
     *
     * Changing this value switches between tabs/sections in the UI.
     * This is a delegated property that reads/writes to the underlying [MutableState].
     */
    var topLevelRoute by topLevelRoute

    /**
     * Returns the list of top-level routes whose back stacks should be rendered.
     *
     * This determines which back stacks are "active" and should have their entries
     * included in the navigation host:
     * - If at the start route: only the start route's stack
     * - Otherwise: both start route and current top-level route's stacks
     *
     * This enables preserving the start route's state even when viewing other tabs.
     */
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}