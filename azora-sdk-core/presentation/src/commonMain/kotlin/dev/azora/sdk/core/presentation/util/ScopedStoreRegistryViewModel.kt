package dev.azora.sdk.core.presentation.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore

/**
 * A ViewModel that manages a registry of [ViewModelStore] instances keyed by string identifiers.
 *
 * This class enables scoped ViewModel management where different parts of the application
 * can have their own independent ViewModelStore. This is particularly useful for:
 * - **Navigation scopes**: Each navigation destination can have its own ViewModelStore
 * - **Dynamic screens**: Screens created dynamically (e.g., tabs, dialogs) can manage
 *   their ViewModels independently
 * - **Feature isolation**: Different features can have isolated ViewModel lifecycles
 *
 * ## Lifecycle Management
 * The registry automatically cleans up all contained ViewModelStores when this ViewModel
 * is cleared, preventing memory leaks. Individual stores can also be cleared manually
 * using [clear] when their associated scope is destroyed.
 *
 * ## Usage
 * ```kotlin
 * @Composable
 * fun ScopedContent(scopeId: String) {
 *     val registryViewModel: ScopedStoreRegistryViewModel = viewModel()
 *     val scopedStore = registryViewModel.getOrCreate(scopeId)
 *
 *     CompositionLocalProvider(
 *         LocalViewModelStoreOwner provides ViewModelStoreOwner { scopedStore }
 *     ) {
 *         // ViewModels created here are scoped to this specific store
 *         FeatureScreen()
 *     }
 *
 *     DisposableEffect(scopeId) {
 *         onDispose {
 *             registryViewModel.clear(scopeId)
 *         }
 *     }
 * }
 * ```
 *
 * @see ViewModelStore The Android architecture component that stores ViewModels
 */
class ScopedStoreRegistryViewModel : ViewModel() {

    private val stores = mutableMapOf<String, ViewModelStore>()

    /**
     * Gets an existing [ViewModelStore] for the given ID, or creates a new one if none exists.
     *
     * The returned store will persist until explicitly cleared via [clear] or until
     * this ViewModel is destroyed.
     *
     * @param id A unique identifier for the scope (e.g., navigation route, tab ID)
     * @return The [ViewModelStore] associated with the given ID
     */
    fun getOrCreate(id: String): ViewModelStore =
        stores.getOrPut(id) { ViewModelStore() }

    /**
     * Clears and removes the [ViewModelStore] associated with the given ID.
     *
     * This should be called when the associated scope is destroyed to free resources.
     * If no store exists for the given ID, this method does nothing.
     *
     * @param id The identifier of the scope to clear
     */
    fun clear(id: String) {
        stores.remove(id)?.clear()
    }

    /**
     * Called when this ViewModel is being destroyed.
     *
     * Clears all contained ViewModelStores to prevent memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        stores.values.forEach { it.clear() }
        stores.clear()
    }
}