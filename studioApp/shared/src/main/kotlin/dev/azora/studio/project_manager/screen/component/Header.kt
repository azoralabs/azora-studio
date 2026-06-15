package org.azora.studio.project_manager.screen.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import azora.azora_studio.app.generated.resources.*
import org.azora.sdk.core.theme.LocalAzoraPalette
import androidx.compose.ui.tooling.preview.Preview
import org.azora.sdk.core.component.button.AzoraButton
import org.azora.sdk.core.theme.palette.AzoraPalette
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun Header(
    onNewProjectClick: () -> Unit
) {
    val palette = LocalAzoraPalette.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.surfaceLow,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title section
            Column {
                Text(
                    text = "Project Browser",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.contentTop
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Create or open a project to get started",
                    style = typography.bodySmall,
                    color = palette.contentLow
                )
            }

            AzoraButton(
                text = "New Project",
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.create_project_icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = AzoraPalette.White
                    )
                },
                onClick = onNewProjectClick
            )
        }
    }
}

@Preview
@Composable
private fun Header_Preview() {
    Header(
        onNewProjectClick = {}
    )
}