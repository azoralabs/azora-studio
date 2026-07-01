package dev.azora.sdk.core.component.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import dev.azora.sdk.core.component.debug.AzoraPreview

/**
 * A vertical group of mutually exclusive [AzoraRadioButton]s.
 *
 * Exactly one [options] entry is [selected] at a time. Selecting a different option invokes
 * [onSelected] with that option.
 *
 * @param options The list of selectable options
 * @param selected The currently selected option
 * @param onSelected Callback invoked with the newly selected option
 * @param modifier Modifier applied to the group column
 * @param enabled Whether the group responds to input
 * @param label Maps an option to its display label (defaults to `toString()`)
 */
@Composable
fun <T> AzoraRadioGroup(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (T) -> String = { it.toString() }
) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            AzoraRadioButton(
                selected = option == selected,
                onClick = { onSelected(option) },
                enabled = enabled,
                label = label(option),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AzoraRadioGroup_Preview() = AzoraPreview {
    AzoraRadioGroup(
        options = listOf("Light", "Dark", "System"),
        selected = "Dark",
        onSelected = {},
        modifier = Modifier.padding(8.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun AzoraRadioGroup_DisabledPreview() = AzoraPreview {
    AzoraRadioGroup(
        options = listOf("Option A", "Option B"),
        selected = "Option A",
        onSelected = {},
        enabled = false,
        modifier = Modifier.padding(8.dp)
    )
}
