package dev.azora.launcher

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import azorastudio.shared.generated.resources.*
import dev.azora.sdk.core.theme.palette.*
import dev.azora.sdk.plugin.core.AzoraPlugin
import org.jetbrains.compose.resources.painterResource

enum class NavSection {
    HOME, LIBRARY, STORE, LEARN
}

@Composable
fun LauncherApp(plugins: List<AzoraPlugin>) {
    var selectedNav by remember { mutableStateOf(NavSection.LIBRARY) }
    var searchQuery by remember { mutableStateOf("") }

    val palette = azoraDarkPalette

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AzoraPalette.Primary,
            onPrimary = AzoraPalette.White,
            secondary = AzoraPalette.Secondary,
            onSecondary = AzoraPalette.White,
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.content,
            onSurface = palette.content
        )
    ) {
        Row(modifier = Modifier.fillMaxSize().background(palette.background)) {
            // Sidebar
            Sidebar(
                selectedNav = selectedNav,
                onNavSelected = { selectedNav = it }
            )

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Top Bar with Search
                TopBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Content based on selected nav
                when (selectedNav) {
                    NavSection.HOME -> HomeContent(plugins)
                    NavSection.LIBRARY -> LibraryContent(plugins, searchQuery)
                    NavSection.STORE -> StoreContent()
                    NavSection.LEARN -> LearnContent()
                }
            }
        }
    }
}

@Composable
private fun Sidebar(
    selectedNav: NavSection,
    onNavSelected: (NavSection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(240.dp)
            .background(AzoraPalette.Neutral95)
            .padding(vertical = 24.dp)
    ) {
        // Logo/Brand
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Image(
                painter = painterResource(Res.drawable.azora_logo),
                contentDescription = "Azora",
                modifier = Modifier.width(256.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation Items
        NavItem(
            icon = painterResource(Res.drawable.ic_home),
            label = "Home",
            selected = selectedNav == NavSection.HOME,
            onClick = { onNavSelected(NavSection.HOME) }
        )
        NavItem(
            icon = painterResource(Res.drawable.ic_apps),
            label = "Library",
            selected = selectedNav == NavSection.LIBRARY,
            onClick = { onNavSelected(NavSection.LIBRARY) }
        )
        NavItem(
            icon = painterResource(Res.drawable.ic_store),
            label = "Store",
            selected = selectedNav == NavSection.STORE,
            onClick = { onNavSelected(NavSection.STORE) }
        )
        NavItem(
            icon = painterResource(Res.drawable.ic_learn),
            label = "Learn",
            selected = selectedNav == NavSection.LEARN,
            onClick = { onNavSelected(NavSection.LEARN) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Settings
        NavItem(
            icon = painterResource(Res.drawable.ic_settings),
            label = "Settings",
            selected = false,
            onClick = { }
        )
    }
}

@Composable
private fun NavItem(
    icon: Painter,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (selected) AzoraPalette.Secondary.copy(alpha = 0.15f) else Color.Transparent
    )
    val contentColor by animateColorAsState(
        if (selected) AzoraPalette.Secondary else AzoraPalette.Neutral40
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(contentColor)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = if (selected) AzoraPalette.Neutral10 else AzoraPalette.Neutral40,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AzoraPalette.Secondary)
            )
        }
    }
}

@Composable
private fun TopBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.weight(1f).height(48.dp),
            placeholder = {
                Text("Search apps...", color = AzoraPalette.Neutral60, fontSize = 14.sp)
            },
            leadingIcon = {
                Image(
                    painter = painterResource(Res.drawable.ic_search),
                    contentDescription = "Search",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(AzoraPalette.Neutral60)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AzoraPalette.Secondary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = AzoraPalette.Neutral80,
                unfocusedContainerColor = AzoraPalette.Neutral80,
                cursorColor = AzoraPalette.Secondary,
                focusedTextColor = AzoraPalette.Neutral10,
                unfocusedTextColor = AzoraPalette.Neutral10
            )
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Notification Icon
        IconButton(
            onClick = { },
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AzoraPalette.Neutral80)
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_notifications),
                contentDescription = "Notifications",
                modifier = Modifier.size(22.dp),
                colorFilter = ColorFilter.tint(AzoraPalette.Neutral40)
            )
        }
    }
}

@Composable
private fun HomeContent(plugins: List<AzoraPlugin>) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Featured Banner
        FeaturedBanner()

        Spacer(modifier = Modifier.height(32.dp))

        // Recent Apps Section
        SectionHeader(title = "Recently Used", actionLabel = "See All")

        Spacer(modifier = Modifier.height(16.dp))

        if (plugins.isEmpty()) {
            EmptyState()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                plugins.take(4).forEach { plugin ->
                    AppCard(
                        plugin = plugin,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AzoraPalette.Secondary,
                        AzoraPalette.Primary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome to Azora",
                    color = AzoraPalette.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Build Amazing\nExperiences",
                    color = AzoraPalette.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 42.sp
                )
            }

            Row {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AzoraPalette.White,
                        contentColor = AzoraPalette.Secondary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Get Started", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = { },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AzoraPalette.White
                    ),
                    border = BorderStroke(1.dp, AzoraPalette.White.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Learn More")
                }
            }
        }
    }
}

@Composable
private fun LibraryContent(plugins: List<AzoraPlugin>, searchQuery: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Library",
                    color = AzoraPalette.Neutral10,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${plugins.size} apps installed",
                    color = AzoraPalette.Neutral60,
                    fontSize = 14.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AzoraPalette.Secondary,
                        selectedLabelColor = AzoraPalette.White
                    )
                )
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text("Installed") }
                )
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text("Updates") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val filteredPlugins = plugins.filter {
            searchQuery.isEmpty() || it.metadata.name.contains(searchQuery, ignoreCase = true)
        }

        if (filteredPlugins.isEmpty()) {
            EmptyState()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredPlugins) { plugin ->
                    AppCard(plugin = plugin)
                }
            }
        }
    }
}

@Composable
private fun AppCard(
    plugin: AzoraPlugin,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .hoverable(interactionSource = remember { MutableInteractionSource() })
            .clickable { },
        colors = CardDefaults.cardColors(containerColor = AzoraPalette.Neutral80),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHovered) 8.dp else 0.dp
        )
    ) {
        Column {
            // App Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                parseAccentColor(plugin.metadata.accentColor),
                                parseAccentColor(plugin.metadata.accentColor).copy(alpha = 0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = plugin.metadata.name.take(2).uppercase(),
                    color = AzoraPalette.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // App Info
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = plugin.metadata.name,
                    color = AzoraPalette.Neutral10,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plugin.metadata.description,
                    color = AzoraPalette.Neutral40,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "v${plugin.metadata.version}",
                        color = AzoraPalette.Neutral60,
                        fontSize = 11.sp
                    )
                    Button(
                        onClick = { },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AzoraPalette.Secondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Launch", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.ic_store),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                colorFilter = ColorFilter.tint(AzoraPalette.Neutral60)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Store Coming Soon",
                color = AzoraPalette.Neutral40,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Browse and install new apps",
                color = AzoraPalette.Neutral60,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun LearnContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.ic_learn),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                colorFilter = ColorFilter.tint(AzoraPalette.Neutral60)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Learn Coming Soon",
                color = AzoraPalette.Neutral40,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tutorials and courses will appear here",
                color = AzoraPalette.Neutral60,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.ic_apps),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                colorFilter = ColorFilter.tint(AzoraPalette.Neutral60)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No Apps Found",
                color = AzoraPalette.Neutral40,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Place plugin JARs in ~/.azora/plugins/",
                color = AzoraPalette.Neutral60,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = AzoraPalette.Secondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(AzoraPalette.White)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Store")
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = AzoraPalette.Neutral10,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (actionLabel != null) {
            TextButton(onClick = { }) {
                Text(
                    text = actionLabel,
                    color = AzoraPalette.Secondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun parseAccentColor(hexColor: String): Color {
    return try {
        Color(hexColor.removePrefix("#").toLong(16) or 0xFF000000)
    } catch (_: Exception) {
        AzoraPalette.Secondary
    }
}
