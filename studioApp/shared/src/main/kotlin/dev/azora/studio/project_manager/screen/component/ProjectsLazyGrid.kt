package org.azora.studio.project_manager.screen.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import org.azora.sdk.core.io.*
import org.azora.sdk.core.project.domain.AzoraProjectModel
import org.azora.sdk.core.theme.LocalAzoraPalette
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.serialization.json.Json

@Composable
fun ProjectsLazyGrid(
    fileSystem: FileSystem,
    onProjectClick: (String) -> Unit,
    onNewProjectClick: () -> Unit
) {
    val palette = LocalAzoraPalette.current
    var projects by remember {
        mutableStateOf<List<Pair<FileInfo, AzoraProjectModel>>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        when (val result = fileSystem.listDirectory("Azora", recursive = false)) {
            is ListResult.Success -> {
                val validProjects = result.files
                    .filter { it.isDirectory }
                    .mapNotNull { fileInfo ->
                        val projectJsonPath = "Azora/${fileInfo.name}/project.azora"
                        when (val readResult = fileSystem.readFromFile(projectJsonPath)) {
                            is FileReadResult.Success -> {
                                try {
                                    val projectData = Json.decodeFromString<AzoraProjectModel>(readResult.content)
                                    fileInfo to projectData
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            is FileReadResult.Error -> null
                        }
                    }
                projects = validProjects
                isLoading = false
            }
            is ListResult.Error -> {
                error = result.message
                isLoading = false
            }
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = palette.primary)
            }
        }
        error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Unable to load projects",
                        style = typography.titleMedium,
                        color = palette.contentTop
                    )

                    Text(
                        text = error ?: "Unknown error",
                        style = typography.bodySmall,
                        color = palette.contentLow
                    )
                }
            }
        }
        else -> {
            if (projects.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "No projects yet",
                            style = typography.headlineSmall,
                            color = palette.contentTop
                        )

                        Text(
                            text = "Create your first project to get started",
                            style = typography.bodyMedium,
                            color = palette.contentLow
                        )

                        Box(Modifier.width(200.dp)) {
                            NewProjectCard(onClick = onNewProjectClick)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.FixedSize(200.dp),
                    contentPadding = PaddingValues(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        NewProjectCard(onClick = onNewProjectClick)
                    }

                    items(projects) { (fileInfo, projectData) ->
                        ProjectCard(
                            projectData = projectData,
                            onClick = { onProjectClick(fileInfo.name) }
                        )
                    }
                }
            }
        }
    }
}