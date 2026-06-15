package dev.azora.sdk.core.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.*
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.PolymorphicSerializer

/**
 * Creates and remembers a [NavigationState] with proper state saving and restoration.
 *
 * This composable sets up the navigation infrastructure with:
 * - **Serializable top-level route**: Survives process death and configuration changes
 * - **Per-tab back stacks**: Independent navigation history for each top-level destination
 * - **Polymorphic serialization**: Supports the sealed [Route] hierarchy
 *
 * ## State Restoration
 * The navigation state is fully serializable, meaning:
 * - Current tab selection is restored after process death
 * - Each tab's back stack is independently restored
 * - Deep links and route arguments are preserved
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun App() {
 *     val serializersConfig = SavedStateConfiguration {
 *         polymorphic(NavKey::class) {
 *             subclass(Route.Intro::class, Route.Intro.serializer())
 *             subclass(Route.Menu::class, Route.Menu.serializer())
 *             // ... register all Route subclasses
 *         }
 *     }
 *
 *     val navigationState = rememberNavigationState(
 *         startRoute = Route.Menu.Map.ViewMap,
 *         topLevelRoutes = setOf(
 *             Route.Menu.Map.ViewMap,
 *             Route.Menu.Incidents.SelectIncident,
 *             Route.Menu.Account.None
 *         ),
 *         serializersConfig = serializersConfig
 *     )
 *
 *     val navigator = Navigator(navigationState)
 *     // Use navigator.navigate() and navigator.goBack()
 * }
 * ```
 *
 * @param startRoute The initial/default route, also used as the "home" destination
 * @param topLevelRoutes Set of routes that act as top-level tabs with their own back stacks
 * @param serializersConfig Kotlinx serialization configuration for the [Route] hierarchy
 * @return A [NavigationState] instance with properly configured back stacks
 *
 * @see NavigationState The state class this creates
 * @see Navigator Controller for manipulating navigation state
 * @see Route The sealed interface defining all navigation destinations
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
    serializersConfig: SavedStateConfiguration
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute,
        topLevelRoutes,
        configuration = serializersConfig,
        serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key ->
        rememberNavBackStack(
            configuration = serializersConfig,
            key
        )
    }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}