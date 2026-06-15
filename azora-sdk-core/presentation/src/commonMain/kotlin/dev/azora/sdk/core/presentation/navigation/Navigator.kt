package dev.azora.sdk.core.presentation.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Navigation controller for managing app-wide navigation state.
 *
 * Provides type-safe navigation methods for navigating between screens and
 * managing the navigation back stack. Supports hierarchical navigation with
 * separate back stacks for top-level destinations.
 *
 * This class works in conjunction with [NavigationState] to maintain navigation
 * state across the application.
 *
 * @property state The current navigation state containing back stacks and routes
 */
class Navigator(val state: NavigationState) {

    /**
     * Navigates to the specified route.
     *
     * Behavior:
     * - If the route is a top-level route (exists in backStacks keys), switches to that tab/section
     * - Otherwise, pushes the route onto the current top-level route's back stack
     *
     * @param route The destination route to navigate to
     */
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    /**
     * Navigates back in the navigation hierarchy.
     *
     * Behavior:
     * - If the current route is a top-level route, navigates to the start route
     * - Otherwise, pops the current route from the back stack
     *
     * @throws IllegalStateException if the back stack for the current top-level route doesn't exist
     */
    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Back stack for ${state.topLevelRoute} doesn't exist")
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}