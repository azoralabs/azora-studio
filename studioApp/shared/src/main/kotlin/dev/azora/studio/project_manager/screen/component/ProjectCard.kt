package org.azora.studio.project_manager.screen.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import azora.azora_studio.app.generated.resources.*
import org.azora.sdk.core.component.debug.AzoraPreview
import org.azora.sdk.core.project.domain.AzoraProjectModel
import org.azora.sdk.core.theme.LocalAzoraPalette
import org.azora.sdk.core.theme.palette.AzoraPalette
import org.jetbrains.compose.resources.painterResource

@Composable
fun ProjectCard(
    projectData: AzoraProjectModel,
    onClick: () -> Unit
) {
    val palette = LocalAzoraPalette.current

    Card(
        onClick = onClick,
        modifier = Modifier.size(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            hoveredElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = palette.surfaceTop
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Thumbnail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(palette.surfaceMid),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.folder_icon),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = AzoraPalette.AccentYellow
                )
            }

            // Project info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = projectData.name,
                    style = typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.contentTop
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = projectData.companyName,
                        style = typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = palette.contentLow,
                        modifier = Modifier.weight(1f)
                    )

                    // Engine version badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.surfaceLow)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = projectData.engineVersion,
                            style = typography.labelSmall,
                            color = palette.contentLow
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ProjectCard_Preview() = AzoraPreview {
    ProjectCard(
        projectData = AzoraProjectModel(
            id = "preview-id",
            name = "My Awesome App",
            companyName = "Azora",
            packageName = "org.azora.awesome",
            version = "1.0.0",
            engineVersion = "0.1.0"
        ),
        onClick = {}
    )
}
