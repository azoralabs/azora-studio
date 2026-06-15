package dev.azora.sdk.core.presentation.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.*

/**
 * A type-safe wrapper for UI text that can represent either a dynamic string
 * or a localized string resource.
 *
 * This sealed interface provides a unified way to handle text in the UI layer,
 * allowing view models to return text that can be either:
 * - A dynamic string (e.g., from an API response, user input, or error message)
 * - A localized string resource (from the app's string resources)
 *
 * Benefits:
 * - Decouple ViewModels from UI framework (Android Context, Compose resources)
 * - Enable testability by avoiding direct resource lookups in business logic
 * - Support both static and dynamic text content in a type-safe manner
 *
 * Usage in ViewModel:
 * ```kotlin
 * val errorMessage: UiText = UiText.DynamicString("Network error")
 * val title: UiText = UiText.Resource(Res.string.app_title)
 * ```
 *
 * Usage in Composable:
 * ```kotlin
 * Text(text = uiText.asString())
 * ```
 */
sealed interface UiText {

    /**
     * Represents a dynamic string value.
     *
     * Use this when the text content is determined at runtime, such as:
     * - API responses
     * - User-generated content
     * - Dynamic error messages
     * - Formatted values
     *
     * @property value The string value to display
     */
    data class DynamicString(val value: String): UiText

    /**
     * Represents a localized string resource.
     *
     * Use this for static strings defined in the app's resources that should
     * be localized based on the user's language preference.
     *
     * @property id The string resource identifier
     * @property args Optional formatting arguments for the string resource
     */
    class Resource(
        val id: StringResource,
        val args: Array<Any> = arrayOf()
    ): UiText

    /**
     * Resolves this [UiText] into a displayable string within a Composable context.
     *
     * This function handles both dynamic strings and string resources,
     * automatically applying localization when needed.
     *
     * @return The resolved string value
     */
    @Composable
    fun asString() = when (this) {
        is DynamicString -> value
        is Resource -> stringResource(
            resource = id,
            *args
        )
    }

    /**
     * Resolves this [UiText] into a displayable string asynchronously.
     *
     * Use this function in non-Composable contexts (e.g., ViewModels, coroutines)
     * where you need to resolve the string value outside of the composition.
     *
     * @return The resolved string value
     */
    suspend fun asStringAsync() = when (this) {
        is DynamicString -> value
        is Resource -> getString(
            resource = id,
            *args
        )
    }
}