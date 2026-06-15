package dev.azora.studio.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.LocalAzoraPalette

@Composable
fun SceneStudioSettingsContent(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit
) {
    val palette = LocalAzoraPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Build & Run",
            color = palette.contentTop,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsRow(
            label = "Use Kotlin Multiplatform",
            description = "When enabled, Run opens a Compose Desktop window instead of the standalone GLFW executable"
        ) {
            ToggleButton(
                checked = state.useKmpRenderer,
                onCheckedChange = { onAction(SettingsAction.SetUseKmpRenderer(it)) }
            )
        }
    }
}
