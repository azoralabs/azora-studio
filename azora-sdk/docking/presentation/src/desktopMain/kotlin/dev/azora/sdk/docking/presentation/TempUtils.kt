package dev.azora.sdk.docking.presentation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Modifier extension that makes an element clickable without visual ripple feedback.
 *
 * This is useful for UI elements where the default Material ripple effect is
 * undesirable, such as custom-styled tabs or compact interactive elements
 * in the dock system.
 *
 * ## Usage
 *
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .clickableNoRipple { onTabClicked() }
 *         .background(tabBackground)
 * )
 * ```
 *
 * @param onClick The callback to invoke when the element is tapped
 * @return A new Modifier with tap detection added
 */
@Composable
internal fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onClick() })
        }
    )
}