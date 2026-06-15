package dev.azora.studio.project_manager.screen.create_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.azora.BuildConfig
import dev.azora.studio.project_manager.*
import dev.azora.sdk.core.component.button.AzoraButton
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.sdk.core.project.domain.ProjectTemplate
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
            text = "Template",
            style = typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = palette.contentTop
        )

        TemplateSelector(
            selected = state.template,
            onSelect = { onAction(ProjectManagerAction.OnTemplateChange(it)) }
        )

        // Optional Ktor server — hidden for the Server template (which is itself a server).
        if (state.template.supportsOptionalServer) {
            ServerCheckbox(
                checked = state.includeServer,
                onCheckedChange = { onAction(ProjectManagerAction.OnIncludeServerChange(it)) }
            )
        }

        AzoraHorizontalDivider()

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
                // The Server template is always a server; others use the checkbox.
                val includeServer =
                    if (state.template.supportsOptionalServer) state.includeServer else true
                val project = AzoraProjectModel(
                    id = Uuid.random().toString(),
                    name = state.projectName.field,
                    companyName = state.companyName.field,
                    packageName = state.packageName.field,
                    version = "0.1.0",
                    engineVersion = BuildConfig.STUDIO_VERSION,
                    template = state.template,
                    includeServer = includeServer
                )
                onAction(ProjectManagerAction.OnCreateProject(project))
            }
        )
    }
}

@Composable
private fun TemplateSelector(
    selected: ProjectTemplate,
    onSelect: (ProjectTemplate) -> Unit
) {
    // 3-per-row grid (built from chunked Rows so it nests inside a scrolling dialog).
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProjectTemplate.entries.chunked(3).forEach { rowTemplates ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTemplates.forEach { template ->
                    TemplateCard(
                        template = template,
                        isSelected = template == selected,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Keep cell widths consistent when the last row isn't full.
                repeat(3 - rowTemplates.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: ProjectTemplate,
    isSelected: Boolean,
    onSelect: (ProjectTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAzoraPalette.current
    Column(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) palette.surfaceLow else palette.surfaceMid)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) palette.primary else palette.surfaceDisabled,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect(template) }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = template.label,
            style = typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) palette.primary else palette.contentTop
        )
        Text(
            text = template.description,
            style = typography.labelSmall,
            color = palette.contentMid,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ServerCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalAzoraPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = palette.primary)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Include Ktor server",
                style = typography.bodyMedium,
                color = palette.contentTop
            )
            Text(
                text = "Adds a Ktor backend module alongside the app",
                style = typography.bodySmall,
                color = palette.contentMid
            )
        }
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