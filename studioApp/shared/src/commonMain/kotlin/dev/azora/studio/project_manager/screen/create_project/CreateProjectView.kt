package dev.azora.studio.project_manager.screen.create_project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.azora.BuildConfig
import dev.azora.studio.project_manager.*
import dev.azora.sdk.core.component.button.AzoraButton
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import kotlin.uuid.Uuid
import dev.azora.sdk.core.component.debug.AzoraPreview
import dev.azora.sdk.core.component.dialog.AzoraDialog
import dev.azora.sdk.core.component.divider.AzoraHorizontalDivider
import dev.azora.sdk.core.component.textfield.AzoraTextField
import dev.azora.sdk.core.theme.LocalAzoraPalette
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ColumnScope.CreateProjectView(
    state: ProjectManagerState,
    onAction: (ProjectManagerAction) -> Unit
) {
    val palette = LocalAzoraPalette.current

    // Header
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Create New Project",
            style = typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = palette.contentTop
        )

        Text(
            text = "Configure your new Azora project",
            style = typography.bodySmall,
            color = palette.contentMid
        )
    }

    AzoraHorizontalDivider(thick = true)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Project Details",
            style = typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = palette.contentTop
        )

        AzoraTextField(
            value = state.projectName.field,
            onValueChange = {
                onAction(ProjectManagerAction.OnProjectNameChange(it))
            },
            title = "Project Name",
            placeholder = "MyAwesomeApp"
        )

        AzoraTextField(
            value = state.companyName.field,
            onValueChange = {
                onAction(ProjectManagerAction.OnCompanyNameChange(it))
            },
            title = "Company / Organization",
            placeholder = "MyCompany"
        )

        AzoraTextField(
            value = state.packageName.field,
            onValueChange = {
                onAction(ProjectManagerAction.OnDomainPathChange(it))
            },
            title = "Package Name"
        )

        Spacer(Modifier)

        // Create Project Button
        AzoraButton(
            text = when {
                state.creating.inProcess -> "Creating..."
                else -> "Create Project"
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.creating.inProcess &&
                    state.projectName.field.isNotBlank() &&
                    state.companyName.field.isNotBlank(),
            onClick = {
                val project = AzoraProjectModel(
                    id = Uuid.random().toString(),
                    name = state.projectName.field,
                    companyName = state.companyName.field,
                    packageName = state.packageName.field,
                    version = "0.1.0",
                    engineVersion = BuildConfig.STUDIO_VERSION
                )
                onAction(ProjectManagerAction.OnCreateProject(project))
            }
        )
    }
}

@Composable
@Preview
private fun CreateProjectView_Preview() = AzoraPreview {
    val palette = LocalAzoraPalette.current

    AzoraDialog(
        contentAlignment = Alignment.Start,
        bottom = {
            Text(
                text = "Project Location",
                style = typography.labelSmall,
                fontWeight = FontWeight.Light,
                color = palette.contentLow
            )

            Text(
                text = "~/Documents/Azora/NewProject",
                style = typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = palette.contentMid
            )
        }
    ) {
        CreateProjectView(
            state = ProjectManagerState(),
            onAction = {}
        )
    }
}

/*





    // Two-column layout
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {


        // Right column - Project Type & Target Platforms
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Type Section
            FormSection(title = "Project Type") {
                val projectTypeItems = ProjectType.entries.map { type ->
                    AzoraDropdownItem(
                        id = type.name,
                        label = type.label,
                        value = type.name
                    )
                }

                AzoraDropdown(
                    items = projectTypeItems,
                    selectedItem = projectTypeItems.find { it.id == state.projectType.name },
                    onChanged = { item, _ ->
                        val type = ProjectType.entries.find { it.name == item.id } ?: ProjectType.APPLICATION
                        onAction(ProjectManagerAction.OnProjectTypeChange(type))
                    },
                    hintText = "Select project type"
                )
            }

            // Target Platforms Section
            FormSection(title = "Target Platforms") {
                PlatformCheckboxGrid(
                    platforms = state.platforms,
                    onPlatformChange = { platform, enabled ->
                        onAction(ProjectManagerAction.OnPlatformChange(platform, enabled))
                    }
                )
            }
        }
    }

    HorizontalDivider(color = AzoraPalette.Neutral70, thickness = 1.dp)

    // Bottom row - Project Location (left) + Create Button (right)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Project Location - left side
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Project Location",
                style = typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = AzoraPalette.Neutral40
            )
            Text(
                text = "~/Documents/Azora/${state.projectName.field.ifBlank { "MyProject" }}",
                style = typography.bodyMedium,
                color = AzoraPalette.Neutral20
            )
        }

        // Create Button - right side
        AzoraButton(
            text = if (state.creating.inProcess) "Creating..." else "Create Project",
            onClick = {
            val projectName = state.projectName.field
            val companyName = state.companyName.field
            val packageName = state.packageName.field

            val mainFeatureId = Uuid.random().toString()
            val mainScreenId = Uuid.random().toString()
            val mainRouteId = Uuid.random().toString()

            val project = ProjectModel(
                id = Uuid.random().toString(),
                name = projectName,
                companyName = companyName,
                packageName = packageName,
                version = "v0.1.0",
                engineVersion = BuildConfig.APP_VERSION,
                activeFeatureId = mainFeatureId,
                activeScreenId = mainScreenId,
                features = dictOf(
                    mainFeatureId to ProjectFeatureModel(
                        id = mainFeatureId,
                        name = "${projectName}Feature",
                        screens = dictOf(
                            mainScreenId to ProjectScreenModel(
                                id = mainScreenId,
                                featureId = mainFeatureId,
                                name = "${projectName}Screen"
                            )
                        ),
                        inheritedFeatureIds = emptySet(),
                        dataClasses = emptyDict(),
                        enums = emptyDict()
                    )
                ),
                navigation = dictOf(
                    mainRouteId to ProjectNavigationModel(
                        id = mainRouteId,
                        screenId = mainScreenId,
                        name = "Main Route",
                        type = NavigationType.STACK,
                        children = emptyDict()
                    )
                )
            )

            onAction(ProjectManagerAction.OnCreateProject(project))
        },
        enabled = !state.creating.inProcess &&
                state.projectName.field.isNotBlank() &&
                state.companyName.field.isNotBlank()
        )
    }*/

/*@Composable
private fun PlatformCheckboxGrid(
    platforms: PlatformSelection,
    onPlatformChange: (Platform, Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlatformCheckbox(
                label = "Android",
                checked = platforms.android,
                onCheckedChange = { onPlatformChange(Platform.ANDROID, it) }
            )
            PlatformCheckbox(
                label = "iOS",
                checked = platforms.ios,
                onCheckedChange = { onPlatformChange(Platform.IOS, it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlatformCheckbox(
                label = "Desktop",
                checked = platforms.desktop,
                onCheckedChange = { onPlatformChange(Platform.DESKTOP, it) }
            )
            PlatformCheckbox(
                label = "Web",
                checked = platforms.webWasm,
                onCheckedChange = { onPlatformChange(Platform.WEB_WASM, it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlatformCheckbox(
                label = "Web (React)",
                checked = platforms.webReact,
                onCheckedChange = { onPlatformChange(Platform.WEB_REACT, it) }
            )
            PlatformCheckbox(
                label = "Server",
                checked = platforms.server,
                onCheckedChange = { onPlatformChange(Platform.SERVER, it) }
            )
        }
    }
}

@Composable
private fun RowScope.PlatformCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AzoraCheckbox(
            isChecked = checked,
            onTap = onCheckedChange
        )
        Text(
            text = label,
            style = typography.bodyMedium,
            color = AzoraPalette.Neutral10
        )
    }
}
*/