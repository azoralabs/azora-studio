package org.azora.studio.settings

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
import org.azora.sdk.core.theme.LocalAzoraPalette

/**
 * Main settings panel with tabbed navigation.
 *
 * @param state Current settings state
 * @param onAction Callback for settings actions
 * @param onLaunchPlugin Callback when user wants to launch a plugin
 */
@Composable
fun SettingsPanel(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onLaunchPlugin: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalAzoraPalette.current
    var selectedTab by remember { mutableStateOf(SettingsTab.GENERAL) }

    Row(modifier = modifier.fillMaxSize()) {
        // Left side - Tab selector
        Column(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .background(palette.surfaceMid)
                .padding(vertical = 8.dp)
        ) {
            SettingsTab.entries.forEach { tab ->
                SettingsTabButton(
                    tab = tab,
                    isSelected = selectedTab == tab,
                    onClick = { selectedTab = tab }
                )
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(palette.surfaceDisabled)
        )

        // Right side - Tab content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(palette.surfaceMid)
        ) {
            when (selectedTab) {
                SettingsTab.GENERAL -> GeneralSettingsContent(
                    state = state,
                    onAction = onAction
                )
                SettingsTab.THEME -> ThemeSettingsContent(
                    state = state,
                    onAction = onAction
                )
                SettingsTab.AZORA_SCRIPT -> AzScriptSettingsContent(
                    state = state,
                    onAction = onAction
                )
                SettingsTab.SCENE_STUDIO -> SceneStudioSettingsContent(
                    state = state,
                    onAction = onAction
                )
                SettingsTab.PLUGINS -> PluginsSettingsContent(
                    onLaunchPlugin = onLaunchPlugin
                )
            }
        }
    }
}

/**
 * Tab button in the settings sidebar.
 */
@Composable
private fun SettingsTabButton(
    tab: SettingsTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = tab.label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
