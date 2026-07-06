package dev.azora.studio.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.LocalAzoraPalette

/** A settings tab contributed by a plugin (id + label); content is rendered by the host via a callback. */
data class PluginSettingsTab(val pluginId: String, val tabId: String, val label: String)

/**
 * Main settings panel with tabbed navigation. Built-in tabs ([SettingsTab]) plus any tabs contributed
 * by loaded plugins ([pluginTabs]) appear in the sidebar; the host renders built-in content directly
 * and delegates plugin tab content via [pluginTabContent].
 */
@Composable
fun SettingsPanel(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onLaunchPlugin: (String) -> Unit = {},
    pluginTabs: List<PluginSettingsTab> = emptyList(),
    pluginTabContent: @Composable (pluginId: String, tabId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val palette = LocalAzoraPalette.current
    var selectedKey by remember { mutableStateOf(SettingsTab.GENERAL.name) }

    /** Stable key for a plugin tab. */
    fun pluginKey(pt: PluginSettingsTab) = "plugin:${pt.pluginId}:${pt.tabId}"

    Row(modifier = modifier.fillMaxSize()) {
        // Left side — Tab selector (builtin + plugin-contributed)
        Column(
            modifier = Modifier.width(100.dp).fillMaxHeight().background(palette.surfaceMid).padding(vertical = 8.dp)
        ) {
            SettingsTab.entries.forEach { tab ->
                SettingsTabButton(label = tab.label, isSelected = selectedKey == tab.name, onClick = { selectedKey = tab.name })
            }
            pluginTabs.forEach { pt ->
                SettingsTabButton(label = pt.label, isSelected = selectedKey == pluginKey(pt), onClick = { selectedKey = pluginKey(pt) })
            }
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(palette.surfaceDisabled))

        // Right side — Tab content
        Box(Modifier.weight(1f).fillMaxHeight().background(palette.surfaceMid)) {
            val builtin = SettingsTab.entries.firstOrNull { it.name == selectedKey }
            if (builtin != null) {
                when (builtin) {
                    SettingsTab.GENERAL -> GeneralSettingsContent(state = state, onAction = onAction)
                    SettingsTab.THEME -> ThemeSettingsContent(state = state, onAction = onAction)
                    SettingsTab.AZORA_SCRIPT -> AzScriptSettingsContent(state = state, onAction = onAction)
                    SettingsTab.SCENE_STUDIO -> SceneStudioSettingsContent(state = state, onAction = onAction)
                    SettingsTab.PLUGINS -> PluginsSettingsContent(onLaunchPlugin = onLaunchPlugin)
                    SettingsTab.LIBRARIES -> LibrariesSettingsContent()
                }
            } else {
                // Plugin-contributed tab
                pluginTabs.firstOrNull { pluginKey(it) == selectedKey }?.let { pt ->
                    pluginTabContent(pt.pluginId, pt.tabId)
                }
            }
        }
    }
}

@Composable
private fun SettingsTabButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        when {
            isSelected -> palette.primary
            isHovered -> palette.surfaceTop
            else -> palette.surfaceMid
        }
    )
    val textColor by animateColorAsState(
        when {
            isSelected -> palette.content
            isHovered -> palette.contentTop
            else -> palette.contentMid
        }
    )

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(backgroundColor)
            .hoverable(interactionSource).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = label, color = textColor, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
