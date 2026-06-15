package dev.azora.studio.project_manager.screen.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import azora.azora_studio.app.generated.resources.*
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.component.wrapper.AzoraDashWrapper
import dev.azora.sdk.core.theme.LocalAzoraPalette
import org.jetbrains.compose.resources.painterResource

@Composable
fun NewProjectCard(
    onClick: () -> Unit
) {
    val palette = LocalAzoraPalette.current

    Card(
        onClick = onClick,
        modifier = Modifier.size(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.surfaceLow
        )
    ) {
        AzoraDashWrapper {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.secondary.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize().weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.create_project_icon),
                            contentDescription = "Create New Project",
                            tint = palette.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "New Project",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = palette.contentTop
                    )

                    Text(
                        text = "Click to create",
                        style = typography.bodySmall,
                        color = palette.contentLow
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun NewProjectCard_Preview() = AzoraPreview {
    NewProjectCard(
        onClick = {}
    )
}