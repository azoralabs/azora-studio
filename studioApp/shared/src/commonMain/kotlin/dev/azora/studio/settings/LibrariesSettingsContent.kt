package dev.azora.studio.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.LocalAzoraPalette
import dev.azora.sdk.library.core.InstalledLibrary
import dev.azora.sdk.library.presentation.LibraryManagerViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

/**
 * Libraries management UI: install/remove library bundles (e.g. the Azora
 * Engine). Installed libraries contribute project templates (such as "App"
 * and "Game") to the create-project dialog.
 */
@Composable
fun LibrariesSettingsContent() {
    val viewModel: LibraryManagerViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val palette = LocalAzoraPalette.current

    // Desktop provides a native picker; on other targets it is unbound (button hidden).
    val filePicker = getKoin().getOrNull<LibraryFilePicker>()

    LaunchedEffect(Unit) {
        viewModel.loadLibraries()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Installed Libraries",
                color = palette.contentTop,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${state.installedLibraries.size} libraries",
                    color = palette.contentLow,
                    fontSize = 11.sp
                )
                if (filePicker != null) {
                    InstallLibraryButton {
                        filePicker.pickLibraryBundle()?.let { path -> viewModel.installLibrary(path) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Libraries are loaded from ~/.azora/libraries/ and contribute project templates (e.g. App, Game).",
            color = palette.contentLow,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = palette.primary,
                    strokeWidth = 2.dp
                )
            }
        }

        state.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.error.copy(alpha = 0.1f))
                    .padding(8.dp)
            ) {
                Text(
                    text = error,
                    color = palette.error,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!state.isLoading && state.installedLibraries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No libraries installed",
                        color = palette.contentMid,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Install a library bundle (folder or .azlib) — e.g. the Azora Engine",
                        color = palette.contentLow,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.installedLibraries.forEach { library ->
                    LibraryCard(
                        library = library,
                        onRemove = { viewModel.uninstallLibrary(library.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(
    library: InstalledLibrary,
    onRemove: () -> Unit
) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        if (isHovered) palette.surfaceTop else palette.surfaceLow
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val accentColor = remember(library.name) {
                    val hash = library.name.hashCode()
                    Color(
                        red = ((hash shr 16) and 0xFF) / 255f * 0.6f + 0.4f,
                        green = ((hash shr 8) and 0xFF) / 255f * 0.6f + 0.4f,
                        blue = (hash and 0xFF) / 255f * 0.6f + 0.4f,
                        alpha = 1f
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = library.name.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = library.name,
                            color = palette.contentTop,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "v${library.version}",
                            color = palette.contentLow,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = library.description,
                        color = palette.contentMid,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (library.templates.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Templates: ${library.templates.joinToString(" · ") { it.label }}",
                            color = palette.contentLow,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            RemoveLibraryButton(onClick = onRemove)
        }
    }
}

@Composable
private fun RemoveLibraryButton(onClick: () -> Unit) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val color by animateColorAsState(if (isHovered) palette.error else palette.contentLow)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Remove",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InstallLibraryButton(onClick: () -> Unit) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        if (isHovered) palette.primary else palette.surfaceLow
    )
    val borderColor by animateColorAsState(
        if (isHovered) palette.primary else palette.surfaceDisabled
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+ Install Library…",
            color = if (isHovered) palette.content else palette.contentTop,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
