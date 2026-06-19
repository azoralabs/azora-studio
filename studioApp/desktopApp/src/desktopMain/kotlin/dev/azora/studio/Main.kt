package dev.azora.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import azora.azora_studio.app.generated.resources.Res as AppRes
import azora.azora_studio.app.generated.resources.*
import dev.azora.studio.azora_nodes.AzoraNodesViewModel
import dev.azora.sdk.core.presentation.undoredo.GlobalUndoRedoCoordinator
import dev.azora.BuildConfig
import dev.azora.canvas.domain.interpreter.ConsoleOutputManager
import dev.azora.sdk.core.domain.preferences.ThemePreference
import dev.azora.sdk.core.domain.preferences.ThemePreferences
import dev.azora.studio.di.initKoin
import dev.azora.studio.run.ProjectRunner
import dev.azora.studio.run.RunTarget
import dev.azora.studio.run.RunTargets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.azora.studio.project_manager.ProjectManagerApp
import dev.azora.sdk.core.domain.util.Res
import dev.azora.sdk.core.project.domain.AzoraProjectModel
import dev.azora.studio.settings.sceneStudioUseKmp
import dev.azora.sdk.core.project.domain.globalConstants
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import dev.azora.sdk.core.theme.*
import dev.azora.sdk.core.theme.palette.*
import dev.azora.sdk.docking.domain.DockAction
import dev.azora.sdk.docking.domain.DockPanelDescriptor
import dev.azora.sdk.docking.domain.DockZone
import dev.azora.sdk.docking.domain.collectPanelIds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource as composeResourcePainter
import org.koin.compose.koinInject
import java.awt.*
import java.awt.Desktop
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.UIManager



private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
private val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
private val isLinux = !isWindows && !isMacOS

// macOS title bar safe area height (traffic lights area)
private const val MACOS_TITLE_BAR_HEIGHT = 28

// Studio toolbar height (same as standard macOS title bar)
private const val EDITOR_TOOLBAR_HEIGHT = 28

private fun isSystemInDarkMode() = try {
    // Check macOS dark mode
    val os = System.getProperty("os.name").lowercase()
    if (os.contains("mac")) {
        val process = ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
            .redirectErrorStream(true)
            .start()
        val result = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        result.equals("Dark", ignoreCase = true)
    } else {
        // For Linux/Windows, check GTK theme or use a fallback
        val lafName = UIManager.getLookAndFeel()?.name?.lowercase() ?: ""
        lafName.contains("dark") || lafName.contains("darcula")
    }
} catch (_: Exception) {
    true // Default to dark mode if detection fails
}

fun main() {
    // Set application name for macOS menu bar
    System.setProperty("apple.awt.application.name", "Azora Studio")

    // Set dock icon on macOS with proper padding
    if (Taskbar.isTaskbarSupported()) {
        val taskbar = Taskbar.getTaskbar()
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            val iconStream = Thread.currentThread().contextClassLoader.getResourceAsStream("azora_icon.png")
            if (iconStream != null) {
                val originalIcon = ImageIO.read(iconStream)
                // Create a larger canvas with padding (icon at 80% size)
                val canvasSize = 128
                val iconSize = (canvasSize * 0.8).toInt()
                val padding = (canvasSize - iconSize) / 2

                val paddedIcon = BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB)
                val g = paddedIcon.createGraphics()
                val scaledIcon = originalIcon.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH)
                g.drawImage(scaledIcon, padding, padding, null)
                g.dispose()

                taskbar.iconImage = paddedIcon
            }
        }
    }

    initKoin {
        modules(dev.azora.studio.di.desktopModule)
    }

    // Initialise a theme up-front so AzoraTheme.current is set before the first frame;
    // the persisted preference is applied reactively once it loads.
    AzoraTheme.apply(if (isSystemInDarkMode()) AzoraTheme.Dark else AzoraTheme.Light)

    application {
        var appState by remember { mutableStateOf<AppWindowState>(AppWindowState.InitialSplash) }
        var loadingState by remember { mutableStateOf(LoadingState()) }

        // Persisted theme preference (System / Light / Dark), restored on each launch.
        val themePreferences: ThemePreferences = koinInject()
        val themeScope = rememberCoroutineScope()
        val themePreference by themePreferences.observeThemePreference()
            .collectAsState(initial = ThemePreference.SYSTEM)
        val systemDark = remember { isSystemInDarkMode() }
        val isDarkMode = when (themePreference) {
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
            ThemePreference.SYSTEM -> systemDark
        }
        val onThemeChange: (ThemePreference) -> Unit = { preference ->
            themeScope.launch { themePreferences.updateThemePreference(preference) }
        }
        LaunchedEffect(isDarkMode) {
            AzoraTheme.apply(if (isDarkMode) AzoraTheme.Dark else AzoraTheme.Light)
        }

        val projectRepository: AzoraProjectRepository = koinInject()
        val splashConsole: ConsoleOutputManager = koinInject()

        // Initial splash loading
        LaunchedEffect(appState) {
            when (appState) {
                is AppWindowState.InitialSplash -> {
                    loadingState = loadingState.copy(
                        currentTask = "Starting Azora Studio...",
                        progress = 0.05f
                    )

                    // AzScript / azora-lang toolchain is disabled for now.

                    val loadingTasks = listOf(
                        "Initializing core systems...",
                        "Loading rendering engine...",
                        "Initializing plugin system...",
                        "Loading UI framework...",
                        "Preparing workspace...",
                        "Starting Azora Studio..."
                    )

                    loadingTasks.forEachIndexed { index, task ->
                        loadingState = loadingState.copy(
                            currentTask = task,
                            progress = 0.1f + (index + 1).toFloat() / loadingTasks.size * 0.85f
                        )
                        delay(200)
                    }

                    loadingState = loadingState.copy(isComplete = true, progress = 1f)
                    delay(200)

                    appState = AppWindowState.ProjectManager
                }

                is AppWindowState.LoadingProject -> {
                    val state = appState as AppWindowState.LoadingProject
                    val projectPath = state.projectPath
                    val projectName = state.projectName

                    // Reset loading state for project loading
                    loadingState = LoadingState()

                    // Step 1: Load project configuration
                    loadingState = loadingState.copy(
                        currentTask = "Loading project configuration...",
                        progress = 0.2f
                    )

                    val projectResult = projectRepository.openProject(projectPath)

                    when (projectResult) {
                        is Res.Success -> {
                            // Step 2: Initialize project resources
                            loadingState = loadingState.copy(
                                currentTask = "Initializing project resources...",
                                progress = 0.4f
                            )
                            delay(100)

                            // Step 3: Load plugins
                            loadingState = loadingState.copy(
                                currentTask = "Loading plugins for $projectName...",
                                progress = 0.6f
                            )
                            delay(100)

                            // Step 4: Prepare editor workspace
                            loadingState = loadingState.copy(
                                currentTask = "Preparing editor workspace...",
                                progress = 0.8f
                            )
                            delay(100)

                            // Step 5: Opening project
                            loadingState = loadingState.copy(
                                currentTask = "Opening $projectName...",
                                progress = 1.0f,
                                isComplete = true
                            )
                            delay(200)

                            appState = AppWindowState.Studio(
                                projectResult.data.copy(projectDirectoryPath = projectPath),
                                projectPath
                            )
                        }

                        is Res.Failure -> {
                            loadingState = loadingState.copy(
                                currentTask = "Failed to load project",
                                isComplete = true
                            )
                            delay(1000)
                            // Return to project manager on failure
                            appState = AppWindowState.ProjectManager
                        }
                    }
                }

                else -> { /* No loading needed */ }
            }
        }

        when (val state = appState) {
            // Initial Splash Screen (undecorated)
            is AppWindowState.InitialSplash -> {
                val splashState = rememberWindowState(
                    size = DpSize(SPLASH_WIDTH.dp, SPLASH_HEIGHT.dp),
                    position = WindowPosition(Alignment.Center)
                )

                Window(
                    onCloseRequest = ::exitApplication,
                    state = splashState,
                    undecorated = true,
                    transparent = false,
                    resizable = false,
                    title = "Azora Studio",
                    icon = painterResource("azora_icon.png")
                ) {
                    val palette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
                    CompositionLocalProvider(LocalAzoraPalette provides palette) {
                        SplashScreen(loadingState = loadingState)
                    }
                }
            }

            // Project Manager Window
            is AppWindowState.ProjectManager -> {
                val windowState = rememberWindowState(
                    size = DpSize(WINDOW_WIDTH.dp, WINDOW_HEIGHT.dp),
                    position = WindowPosition(Alignment.Center)
                )

                Window(
                    onCloseRequest = ::exitApplication,
                    title = "Azora Studio ${BuildConfig.STUDIO_VERSION}",
                    state = windowState,
                    icon = painterResource("azora_icon.png")
                ) {
                    // Set native title bar color and text color on macOS/Linux
                    LaunchedEffect(isDarkMode) {
                        if (!isWindows) {
                            val titleBarColor = if (isDarkMode) {
                                Color(
                                    (AzoraPalette.Neutral90.red * 255).toInt(),
                                    (AzoraPalette.Neutral90.green * 255).toInt(),
                                    (AzoraPalette.Neutral90.blue * 255).toInt()
                                )
                            } else {
                                Color(
                                    (azoraLightPalette.surfaceTop.red * 255).toInt(),
                                    (azoraLightPalette.surfaceTop.green * 255).toInt(),
                                    (azoraLightPalette.surfaceTop.blue * 255).toInt()
                                )
                            }
                            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                            // Set title bar text color: dark appearance = light text (Neutral10), light appearance = dark text
                            window.rootPane.putClientProperty(
                                "apple.awt.windowAppearance",
                                if (isDarkMode) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
                            )
                            window.background = titleBarColor
                        }
                    }

                    window.minimumSize = Dimension(
                        MIN_WINDOW_WIDTH.dp.value.toInt(),
                        MIN_WINDOW_HEIGHT.dp.value.toInt()
                    )

                    MenuBar {
                        Menu("View") {
                            Menu("Theme") {
                                RadioButtonItem(
                                    text = "System",
                                    selected = themePreference == ThemePreference.SYSTEM,
                                    onClick = { onThemeChange(ThemePreference.SYSTEM) }
                                )
                                RadioButtonItem(
                                    text = "Light",
                                    selected = themePreference == ThemePreference.LIGHT,
                                    onClick = { onThemeChange(ThemePreference.LIGHT) }
                                )
                                RadioButtonItem(
                                    text = "Dark",
                                    selected = themePreference == ThemePreference.DARK,
                                    onClick = { onThemeChange(ThemePreference.DARK) }
                                )
                            }
                        }
                    }

                    // Safe area padding for macOS transparent title bar
                    val safeAreaTop = if (!isWindows) MACOS_TITLE_BAR_HEIGHT.dp else 0.dp

                    Box(modifier = Modifier.fillMaxSize().padding(top = safeAreaTop)) {
                        ProjectManagerApp(
                            isDarkMode = isDarkMode,
                            onProjectSelected = { projectPath, projectName ->
                                appState = AppWindowState.LoadingProject(projectPath, projectName)
                            }
                        )
                    }
                }
            }

            // Project Loading Splash (undecorated)
            is AppWindowState.LoadingProject -> {
                val splashState = rememberWindowState(
                    size = DpSize(SPLASH_WIDTH.dp, SPLASH_HEIGHT.dp),
                    position = WindowPosition(Alignment.Center)
                )

                Window(
                    onCloseRequest = ::exitApplication,
                    state = splashState,
                    undecorated = true,
                    transparent = false,
                    resizable = false,
                    title = "Loading Project",
                    icon = painterResource("azora_icon.png")
                ) {
                    val palette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
                    CompositionLocalProvider(LocalAzoraPalette provides palette) {
                        SplashScreen(loadingState = loadingState)
                    }
                }
            }

            // Studio Window (maximized) with Native Floating Windows
            is AppWindowState.Studio -> {
                val windowState = rememberWindowState(
                    placement = WindowPlacement.Maximized
                )

                // Inject DockStateManager singleton directly - shared between main window and floating windows
                val dockStateManager: dev.azora.sdk.docking.domain.DockStateManager = koinInject()
                // Collect dock state directly for immediate updates across all windows
                val dockState by dockStateManager.state.collectAsState()

                // Track screen position and density for coordinate conversion
                var screenOffsetX by remember { mutableStateOf(0f) }
                var screenOffsetY by remember { mutableStateOf(0f) }
                var mainWindowDensity by remember { mutableStateOf(1f) }

                // Callback to close project and return to project manager
                val onCloseProject = remember { { appState = AppWindowState.ProjectManager } }

                // Settings, Global Constants, About, and Game Preview window states
                var showSettingsWindow by remember { mutableStateOf(false) }
                var showGlobalConstantsWindow by remember { mutableStateOf(false) }
                var showAboutDialog by remember { mutableStateOf(false) }
                var showGameWindow by remember { mutableStateOf(false) }

                // Inject GlobalUndoRedoCoordinator
                val undoRedoCoordinator: GlobalUndoRedoCoordinator = koinInject()
                val canUndo by undoRedoCoordinator.canUndo.collectAsState()
                val canRedo by undoRedoCoordinator.canRedo.collectAsState()

                // Inject ConsoleOutputManager for build/run logging
                val consoleOutputManager: ConsoleOutputManager = koinInject()

                // Project runner — streams the generated project's run output to the Console panel
                val projectRunner = remember(consoleOutputManager) { ProjectRunner(consoleOutputManager) }
                val projectDir = remember(state.projectPath) {
                    java.io.File(System.getProperty("user.home") + "/Documents", state.projectPath)
                }
                val runScope = rememberCoroutineScope()
                var runTargets by remember(state.project.template) { mutableStateOf<List<RunTarget>>(emptyList()) }
                var selectedTarget by remember(state.project.template) { mutableStateOf<RunTarget?>(null) }
                val templateRunnable = RunTargets.isRunnable(state.project.template)
                val refreshTargets: () -> Unit = {
                    runScope.launch {
                        val targets = withContext(Dispatchers.IO) { RunTargets.targetsFor(state.project.template) }
                        runTargets = targets
                        if (selectedTarget == null || targets.none { it.id == selectedTarget?.id }) {
                            selectedTarget = targets.firstOrNull()
                        }
                    }
                }
                LaunchedEffect(state.project.template) { refreshTargets() }
                val onRunProject: () -> Unit = {
                    selectedTarget?.let { projectRunner.run(projectDir, it, state.project.packageName) }
                }
                val onStopProject: () -> Unit = { projectRunner.stop() }

                // Inject PluginManager for menu and windows
                val pluginManager: dev.azora.sdk.plugin.presentation.PluginManager = koinInject()
                val installedPlugins by pluginManager.installedPlugins.collectAsState()
                val enabledPlugins = installedPlugins.filter { it.enabled }

                // Load external plugins on startup
                LaunchedEffect(Unit) {
                    pluginManager.loadInstalledPlugins()
                }

                // Main Studio Window - undecorated with custom title bar on Windows only
                Window(
                    onCloseRequest = ::exitApplication,
                    title = "${state.project.name} - Azora Studio ${BuildConfig.STUDIO_VERSION}",
                    state = windowState,
                    icon = painterResource("azora_icon.png"),
                    undecorated = isWindows,
                    transparent = false
                ) {
                    // Set native title bar properties on macOS/Linux (must be done immediately)
                    DisposableEffect(Unit) {
                        if (!isWindows) {
                            window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                            window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                            // Hide native title - we show our own centered title in the toolbar
                            window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
                        }
                        onDispose { }
                    }

                    // Update title bar color based on theme
                    LaunchedEffect(isDarkMode) {
                        if (!isWindows) {
                            val titleBarColor = if (isDarkMode) {
                                java.awt.Color(
                                    (AzoraPalette.Neutral90.red * 255).toInt(),
                                    (AzoraPalette.Neutral90.green * 255).toInt(),
                                    (AzoraPalette.Neutral90.blue * 255).toInt()
                                )
                            } else {
                                java.awt.Color(
                                    (azoraLightPalette.surfaceTop.red * 255).toInt(),
                                    (azoraLightPalette.surfaceTop.green * 255).toInt(),
                                    (azoraLightPalette.surfaceTop.blue * 255).toInt()
                                )
                            }
                            // Set title bar text color: dark appearance = light text (Neutral10), light appearance = dark text
                            window.rootPane.putClientProperty(
                                "apple.awt.windowAppearance",
                                if (isDarkMode) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
                            )
                            window.background = titleBarColor
                        }
                    }

                    // Built-in panel descriptors
                    val builtInPanels = remember {
                        listOf(
                            DockPanelDescriptor(id = "project", title = "Project", minimumWidth = 200f, minimumHeight = 150f),
                            DockPanelDescriptor(id = "console", title = "Console", minimumWidth = 200f, minimumHeight = 100f),
                            DockPanelDescriptor(id = "problems", title = "Problems", minimumWidth = 200f, minimumHeight = 100f)
                        )
                    }

                    // Plugin panel menu items: one DockPanelDescriptor per group or ungrouped panel
                    val pluginMenuItems: List<DockPanelDescriptor> = remember(enabledPlugins) {
                        val items = mutableListOf<DockPanelDescriptor>()

                        enabledPlugins.forEach { plugin ->
                            val panels = pluginManager.getPluginPanels(plugin.id)
                            if (panels.isNotEmpty()) {
                                // Group panels by group name
                                val grouped = panels.filter { it.group != null }.groupBy { it.group!! }
                                val ungrouped = panels.filter { it.group == null }

                                // Each group becomes a single dock panel
                                grouped.forEach { (groupName, _) ->
                                    items.add(
                                        DockPanelDescriptor(
                                            id = "plugin_group_${plugin.id}_${groupName}",
                                            title = groupName,
                                            minimumWidth = 600f,
                                            minimumHeight = 300f
                                        )
                                    )
                                }

                                // Ungrouped panels: individual entries
                                ungrouped.forEach { panel ->
                                    items.add(
                                        DockPanelDescriptor(
                                            id = "plugin_${plugin.id}_${panel.id}",
                                            title = panel.title,
                                            minimumWidth = panel.minimumWidth,
                                            minimumHeight = panel.minimumHeight
                                        )
                                    )
                                }
                            } else {
                                items.add(
                                    DockPanelDescriptor(
                                        id = "plugin_${plugin.id}",
                                        title = plugin.name,
                                        minimumWidth = 400f,
                                        minimumHeight = 300f
                                    )
                                )
                            }
                        }

                        items
                    }

                    // Register macOS application menu handlers (About, Preferences)
                    LaunchedEffect(Unit) {
                        if (Desktop.isDesktopSupported()) {
                            val desktop = Desktop.getDesktop()
                            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                                desktop.setAboutHandler { showAboutDialog = true }
                            }
                            if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
                                desktop.setPreferencesHandler { showSettingsWindow = true }
                            }
                        }
                    }

                    // OS Menu Bar
                    MenuBar {
                        Menu("File") {
                            Item("Close Project", onClick = onCloseProject)
                        }
                        Menu("Edit") {
                            Item(
                                "Undo",
                                shortcut = KeyShortcut(Key.Z, meta = true),
                                enabled = canUndo,
                                onClick = { undoRedoCoordinator.undo() }
                            )
                            Item(
                                "Redo",
                                shortcut = KeyShortcut(Key.Z, meta = true, shift = true),
                                enabled = canRedo,
                                onClick = { undoRedoCoordinator.redo() }
                            )
                            Separator()
                            Item(
                                "Global Constants",
                                shortcut = KeyShortcut(Key.G, meta = true, shift = true),
                                onClick = { showGlobalConstantsWindow = true }
                            )
                        }
                        Menu("View") {
                            Menu("Theme") {
                                RadioButtonItem(
                                    text = "System",
                                    selected = themePreference == ThemePreference.SYSTEM,
                                    onClick = { onThemeChange(ThemePreference.SYSTEM) }
                                )
                                RadioButtonItem(
                                    text = "Light",
                                    selected = themePreference == ThemePreference.LIGHT,
                                    onClick = { onThemeChange(ThemePreference.LIGHT) }
                                )
                                RadioButtonItem(
                                    text = "Dark",
                                    selected = themePreference == ThemePreference.DARK,
                                    onClick = { onThemeChange(ThemePreference.DARK) }
                                )
                            }
                            Separator()
                            Menu("Windows") {
                                // Helper to show or add a single panel
                                fun showOrAddPanel(descriptor: DockPanelDescriptor) {
                                    val layout = dockState.layout
                                    val floatingWindow = layout.floatingWindows.find { window ->
                                        window.content.collectPanelIds().contains(descriptor.id)
                                    }
                                    when {
                                        floatingWindow != null -> {
                                            dockStateManager.dispatch(
                                                DockAction.DockFloatingWindow(floatingWindow.id, null, DockZone.CENTER)
                                            )
                                        }
                                        layout.panelDescriptors.containsKey(descriptor.id) -> {
                                            dockStateManager.dispatch(DockAction.SelectPanel(descriptor.id))
                                        }
                                        else -> {
                                            dockStateManager.dispatch(
                                                DockAction.AddPanel(descriptor, null, DockZone.CENTER)
                                            )
                                        }
                                    }
                                }

                                // Built-in panels (shown individually)
                                builtInPanels.forEach { descriptor ->
                                    Item(descriptor.title, onClick = { showOrAddPanel(descriptor) })
                                }

                                // Plugin panels (each group is a single panel)
                                if (pluginMenuItems.isNotEmpty()) {
                                    Separator()
                                    pluginMenuItems.forEach { descriptor ->
                                        Item(descriptor.title, onClick = { showOrAddPanel(descriptor) })
                                    }
                                }
                            }
                        }
                        Menu("Build") {
                            // Read the latest project from repository (settings may have changed since load)
                            fun latestProject(): AzoraProjectModel {
                                return runBlocking {
                                    when (val r = projectRepository.getProject()) {
                                        is Res.Success -> r.data.copy(projectDirectoryPath = state.projectPath)
                                        is Res.Failure -> state.project
                                    }
                                }
                            }
                            Item(
                                "Build Project",
                                shortcut = KeyShortcut(Key.B, meta = true),
                                onClick = {
                                    val project = latestProject()
                                    var handled = false
                                    for (plugin in enabledPlugins) {
                                        val loaded = pluginManager.getLoadedPlugin(plugin.id)
                                        if (loaded != null) {
                                            loaded.handleAction("build", project)
                                            handled = true
                                        }
                                    }
                                    if (!handled) {
                                        consoleOutputManager.error("Build: no plugins loaded to handle build action")
                                    }
                                }
                            )
                            Item(
                                "Run",
                                shortcut = KeyShortcut(Key.R, meta = true),
                                onClick = {
                                    val project = latestProject()
                                    if (project.settings.sceneStudioUseKmp) {
                                        consoleOutputManager.info("Running game (KMP)...")
                                        showGameWindow = true
                                    } else {
                                        var handled = false
                                        for (plugin in enabledPlugins) {
                                            val loaded = pluginManager.getLoadedPlugin(plugin.id)
                                            if (loaded != null) {
                                                loaded.handleAction("run", project)
                                                handled = true
                                            }
                                        }
                                        if (!handled) {
                                            consoleOutputManager.error("Run: no plugins loaded to handle run action")
                                        }
                                    }
                                }
                            )
                            Item(
                                "Build & Run",
                                shortcut = KeyShortcut(Key.R, meta = true, shift = true),
                                onClick = {
                                    val project = latestProject()
                                    // Build
                                    var buildHandled = false
                                    for (plugin in enabledPlugins) {
                                        val loaded = pluginManager.getLoadedPlugin(plugin.id)
                                        if (loaded != null) {
                                            loaded.handleAction("build", project)
                                            buildHandled = true
                                        }
                                    }
                                    if (!buildHandled) {
                                        consoleOutputManager.error("Build & Run: no plugins loaded")
                                        return@Item
                                    }
                                    // Run
                                    if (project.settings.sceneStudioUseKmp) {
                                        consoleOutputManager.info("Running game (KMP)...")
                                        showGameWindow = true
                                    } else {
                                        for (plugin in enabledPlugins) {
                                            val loaded = pluginManager.getLoadedPlugin(plugin.id)
                                            if (loaded != null) {
                                                loaded.handleAction("run", project)
                                            }
                                        }
                                    }
                                }
                            )
                            Separator()
                            Item(
                                "Clean Project",
                                onClick = {
                                    val project = latestProject()
                                    var handled = false
                                    for (plugin in enabledPlugins) {
                                        val loaded = pluginManager.getLoadedPlugin(plugin.id)
                                        if (loaded != null) {
                                            loaded.handleAction("clean", project)
                                            handled = true
                                        }
                                    }
                                    if (!handled) {
                                        consoleOutputManager.error("Clean: no plugins loaded")
                                    }
                                }
                            )
                            Separator()
                            Item(
                                "Hot Reload",
                                shortcut = KeyShortcut(Key.H, meta = true, shift = true),
                                enabled = showGameWindow,
                                onClick = {
                                    consoleOutputManager.info("Hot Reload...")
                                    var handled = false
                                    for (plugin in enabledPlugins) {
                                        val loaded = pluginManager.getLoadedPlugin(plugin.id)
                                        if (loaded != null) {
                                            loaded.handleAction("hot_reload", latestProject())
                                            handled = true
                                        }
                                    }
                                    if (!handled) {
                                        consoleOutputManager.error("Hot Reload: no plugins loaded")
                                    }
                                }
                            )
                        }
                        Menu("Plugins") {
                            Item(
                                "Manage Plugins...",
                                onClick = { showSettingsWindow = true }
                            )
                        }
                    }

                    window.minimumSize = Dimension(
                        MIN_WINDOW_WIDTH.dp.value.toInt(),
                        MIN_WINDOW_HEIGHT.dp.value.toInt()
                    )

                    // Track window position for screen coordinate conversion
                    // Use AWT window location for accurate screen coordinates
                    // Note: positionInRoot() in DockTabBar already accounts for the title bar
                    // since the editor content is laid out below it in the Column,
                    // so we only add the window's screen position here.

                    // Use DisposableEffect with component listener for accurate window position tracking
                    DisposableEffect(window) {
                        fun updateScreenOffset() {
                            try {
                                // Use locationOnScreen for accurate screen coordinates
                                // This accounts for menu bar and other screen insets on macOS
                                val location = window.locationOnScreen
                                screenOffsetX = location.x.toFloat()
                                screenOffsetY = location.y.toFloat()
                            } catch (_: Exception) {
                                screenOffsetX = window.x.toFloat()
                                screenOffsetY = window.y.toFloat()
                            }
                        }

                        val listener = object : java.awt.event.ComponentAdapter() {
                            override fun componentMoved(e: java.awt.event.ComponentEvent) {
                                updateScreenOffset()
                            }
                            override fun componentResized(e: java.awt.event.ComponentEvent) {
                                updateScreenOffset()
                            }
                        }
                        window.addComponentListener(listener)
                        // Initial position
                        updateScreenOffset()
                        onDispose {
                            window.removeComponentListener(listener)
                        }
                    }

                    // Capture density for coordinate conversion
                    val density = LocalDensity.current.density
                    SideEffect {
                        mainWindowDensity = density
                    }

                    val palette = if (isDarkMode) azoraDarkPalette else azoraLightPalette

                    // Track fullscreen state from WindowPlacement
                    val isFullscreen = windowState.placement == WindowPlacement.Fullscreen

                    CompositionLocalProvider(LocalAzoraPalette provides palette) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(palette.background)
                        ) {
                            // Custom title bar only on Windows (macOS/Linux use native title bar)
                            if (isWindows) {
                                WindowDraggableArea {
                                    MainWindowTitleBar(
                                        title = "${state.project.name} - Azora Studio ${BuildConfig.STUDIO_VERSION}",
                                        isDarkMode = isDarkMode,
                                        themePreference = themePreference,
                                        onThemeChange = onThemeChange,
                                        isMaximized = windowState.placement == WindowPlacement.Maximized,
                                        isFullscreen = isFullscreen,
                                        onMinimize = { windowState.isMinimized = true },
                                        onMaximize = {
                                            windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                                                WindowPlacement.Floating
                                            } else {
                                                WindowPlacement.Maximized
                                            }
                                        },
                                        onFullscreen = {
                                            windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                                                WindowPlacement.Maximized
                                            } else {
                                                WindowPlacement.Fullscreen
                                            }
                                        },
                                        onClose = ::exitApplication,
                                        canUndo = canUndo,
                                        canRedo = canRedo,
                                        onUndo = { undoRedoCoordinator.undo() },
                                        onRedo = { undoRedoCoordinator.redo() },
                                        project = state.project,
                                        pluginManager = pluginManager,
                                        enabledPlugins = enabledPlugins,
                                        onPlay = { showGameWindow = true },
                                        onStop = { showGameWindow = false },
                                        runnable = templateRunnable,
                                        runTargets = runTargets,
                                        selectedTarget = selectedTarget,
                                        onSelectTarget = { selectedTarget = it },
                                        onRefreshTargets = refreshTargets,
                                        isProjectRunning = projectRunner.isRunning,
                                        onRunProject = onRunProject,
                                        onStopProject = onStopProject
                                    )
                                }
                            } else {
                                // macOS/Linux toolbar with centered title and theme toggle
                                MacOSToolbar(
                                    title = "${state.project.name} - Azora Studio ${BuildConfig.STUDIO_VERSION}",
                                    isDarkMode = isDarkMode,
                                    themePreference = themePreference,
                                    onThemeChange = onThemeChange,
                                    canUndo = canUndo,
                                    canRedo = canRedo,
                                    onUndo = { undoRedoCoordinator.undo() },
                                    onRedo = { undoRedoCoordinator.redo() },
                                    project = state.project,
                                    pluginManager = pluginManager,
                                    enabledPlugins = enabledPlugins,
                                    onPlay = { showGameWindow = true },
                                    onStop = { showGameWindow = false },
                                    runnable = templateRunnable,
                                    runTargets = runTargets,
                                    selectedTarget = selectedTarget,
                                    onSelectTarget = { selectedTarget = it },
                                    onRefreshTargets = refreshTargets,
                                    isProjectRunning = projectRunner.isRunning,
                                    onRunProject = onRunProject,
                                    onStopProject = onStopProject
                                )
                            }

                            // Studio content
                            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                                StudioAppWithStateExport(
                                    isDarkMode = isDarkMode,
                                    project = state.project,
                                    projectPath = state.projectPath,
                                    screenOffsetX = screenOffsetX,
                                    screenOffsetY = screenOffsetY,
                                    pluginManager = pluginManager,
                                    enabledPlugins = enabledPlugins,
                                    onDockStateChanged = { _, _ -> /* State is already shared via dockStateManager */ }
                                )
                            }
                        }
                    }
                }

                // Settings Window
                if (showSettingsWindow) {
                    val settingsWindowState = rememberWindowState(
                        size = DpSize(700.dp, 500.dp),
                        position = WindowPosition(Alignment.Center)
                    )

                    Window(
                        onCloseRequest = { showSettingsWindow = false },
                        title = "Settings",
                        state = settingsWindowState,
                        icon = painterResource("azora_icon.png"),
                        resizable = true
                    ) {
                        // Set native title bar color and text color on macOS/Linux
                        LaunchedEffect(isDarkMode) {
                            if (!isWindows) {
                                val titleBarColor = if (isDarkMode) {
                                    java.awt.Color(
                                        (AzoraPalette.Neutral90.red * 255).toInt(),
                                        (AzoraPalette.Neutral90.green * 255).toInt(),
                                        (AzoraPalette.Neutral90.blue * 255).toInt()
                                    )
                                } else {
                                    java.awt.Color(
                                        (azoraLightPalette.surfaceTop.red * 255).toInt(),
                                        (azoraLightPalette.surfaceTop.green * 255).toInt(),
                                        (azoraLightPalette.surfaceTop.blue * 255).toInt()
                                    )
                                }
                                window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                                window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                                // Set title bar text color: dark appearance = light text (Neutral10), light appearance = dark text
                                window.rootPane.putClientProperty(
                                    "apple.awt.windowAppearance",
                                    if (isDarkMode) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
                                )
                                window.background = titleBarColor
                            }
                        }

                        window.minimumSize = Dimension(500, 400)

                        val settingsPalette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
                        val settingsTypography = azoraTypography(dev.azora.sdk.core.theme.font.TTRoundsNeue)

                        // Safe area padding for macOS transparent title bar
                        val safeAreaTop = if (!isWindows) MACOS_TITLE_BAR_HEIGHT.dp else 0.dp

                        CompositionLocalProvider(
                            LocalAzoraPalette provides settingsPalette,
                            LocalAzoraTypography provides settingsTypography
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(settingsPalette.background)
                                    .padding(top = safeAreaTop)
                            ) {
                                dev.azora.studio.settings.SettingsScreen(
                                    projectPath = state.projectPath,
                                    onLaunchPlugin = { pluginId ->
                                        // Add plugin as a dock panel
                                        val plugin = enabledPlugins.find { it.id == pluginId }
                                        if (plugin != null) {
                                            val panelId = "plugin_${plugin.id}"
                                            val descriptor = DockPanelDescriptor(
                                                id = panelId,
                                                title = plugin.name,
                                                minimumWidth = 400f,
                                                minimumHeight = 300f
                                            )
                                            dockStateManager.dispatch(
                                                DockAction.AddPanel(descriptor, null, DockZone.CENTER)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Global Constants Window
                if (showGlobalConstantsWindow) {
                    val globalConstantsWindowState = rememberWindowState(
                        size = DpSize(500.dp, 600.dp),
                        position = WindowPosition(Alignment.Center)
                    )

                    Window(
                        onCloseRequest = { showGlobalConstantsWindow = false },
                        title = "Global Constants",
                        state = globalConstantsWindowState,
                        icon = painterResource("azora_icon.png"),
                        resizable = true
                    ) {
                        // Set native title bar color and text color on macOS/Linux
                        LaunchedEffect(isDarkMode) {
                            if (!isWindows) {
                                val titleBarColor = if (isDarkMode) {
                                    java.awt.Color(
                                        (AzoraPalette.Neutral90.red * 255).toInt(),
                                        (AzoraPalette.Neutral90.green * 255).toInt(),
                                        (AzoraPalette.Neutral90.blue * 255).toInt()
                                    )
                                } else {
                                    java.awt.Color(
                                        (azoraLightPalette.surfaceTop.red * 255).toInt(),
                                        (azoraLightPalette.surfaceTop.green * 255).toInt(),
                                        (azoraLightPalette.surfaceTop.blue * 255).toInt()
                                    )
                                }
                                window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                                window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                                window.rootPane.putClientProperty(
                                    "apple.awt.windowAppearance",
                                    if (isDarkMode) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
                                )
                                window.background = titleBarColor
                            }
                        }

                        window.minimumSize = Dimension(400, 300)

                        val globalConstantsPalette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
                        val globalConstantsTypography = azoraTypography(dev.azora.sdk.core.theme.font.TTRoundsNeue)

                        // Safe area padding for macOS transparent title bar
                        val safeAreaTop = if (!isWindows) MACOS_TITLE_BAR_HEIGHT.dp else 0.dp

                        CompositionLocalProvider(
                            LocalAzoraPalette provides globalConstantsPalette,
                            LocalAzoraTypography provides globalConstantsTypography
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(globalConstantsPalette.background)
                                    .padding(top = safeAreaTop)
                            ) {
                                dev.azora.studio.global_constants.GlobalConstantsScreen(
                                    projectPath = state.projectPath
                                )
                            }
                        }
                    }
                }

                // About Dialog
                if (showAboutDialog) {
                    val aboutWindowState = rememberWindowState(
                        size = DpSize(400.dp, 300.dp),
                        position = WindowPosition(Alignment.Center)
                    )

                    Window(
                        onCloseRequest = { showAboutDialog = false },
                        title = "About Azora Studio",
                        state = aboutWindowState,
                        icon = painterResource("azora_icon.png"),
                        resizable = false
                    ) {
                        // Set native title bar color and text color on macOS/Linux
                        LaunchedEffect(isDarkMode) {
                            if (!isWindows) {
                                val titleBarColor = if (isDarkMode) {
                                    java.awt.Color(
                                        (AzoraPalette.Neutral90.red * 255).toInt(),
                                        (AzoraPalette.Neutral90.green * 255).toInt(),
                                        (AzoraPalette.Neutral90.blue * 255).toInt()
                                    )
                                } else {
                                    java.awt.Color(
                                        (azoraLightPalette.surfaceTop.red * 255).toInt(),
                                        (azoraLightPalette.surfaceTop.green * 255).toInt(),
                                        (azoraLightPalette.surfaceTop.blue * 255).toInt()
                                    )
                                }
                                window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                                window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                                // Set title bar text color: dark appearance = light text (Neutral10), light appearance = dark text
                                window.rootPane.putClientProperty(
                                    "apple.awt.windowAppearance",
                                    if (isDarkMode) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua"
                                )
                                window.background = titleBarColor
                            }
                        }

                        val aboutPalette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
                        val aboutTypography = azoraTypography(dev.azora.sdk.core.theme.font.TTRoundsNeue)

                        // Safe area padding for macOS transparent title bar
                        val safeAreaTop = if (!isWindows) MACOS_TITLE_BAR_HEIGHT.dp else 0.dp

                        CompositionLocalProvider(
                            LocalAzoraPalette provides aboutPalette,
                            LocalAzoraTypography provides aboutTypography
                        ) {
                            AboutDialog(
                                onDismiss = { showAboutDialog = false },
                                safeAreaTop = safeAreaTop
                            )
                        }
                    }
                }

                // Game Window (standalone engine, provided by Scene Studio plugin)
                if (showGameWindow) {
                    val gameWindowState = rememberWindowState(
                        size = DpSize(1280.dp, 720.dp),
                        position = WindowPosition(Alignment.Center)
                    )

                    Window(
                        onCloseRequest = {
                            showGameWindow = false
                        },
                        title = "${state.project.name} \u2014 Game",
                        state = gameWindowState,
                        icon = painterResource("azora_icon.png"),
                        resizable = true
                    ) {
                        val gamePalette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
                        val gameTypography = azoraTypography(dev.azora.sdk.core.theme.font.TTRoundsNeue)

                        CompositionLocalProvider(
                            LocalAzoraPalette provides gamePalette,
                            LocalAzoraTypography provides gameTypography
                        ) {
                            pluginManager.getPluginPanelContent("dev.azora.scene_studio", "standalone_game")
                                ?.invoke(state.project)
                        }
                    }
                }

                // Native Floating Windows (real OS windows)
                // Uses dockState collected directly from DockStateManager singleton for immediate updates
                val floatingPalette = if (isDarkMode) azoraDarkPalette else azoraLightPalette
                val floatingTypography = azoraTypography(dev.azora.sdk.core.theme.font.TTRoundsNeue)

                // Create a stable callback for dock actions using the singleton DockStateManager
                val onDockAction = remember(dockStateManager) {
                    { action: DockAction -> dockStateManager.dispatch(action) }
                }

                // Provide theming for floating windows
                CompositionLocalProvider(
                    LocalAzoraPalette provides floatingPalette,
                    LocalAzoraTypography provides floatingTypography
                ) {
                    dev.azora.studio.editor.StudioFloatingWindowsProvider(
                        projectPath = state.projectPath,
                        dockState = dockState,
                        onAction = { action ->
                            when (action) {
                                is dev.azora.studio.editor.StudioAction.DockAction -> onDockAction(action.action)
                            }
                        },
                        project = state.project,
                        pluginManager = pluginManager,
                        enabledPlugins = enabledPlugins
                    ) {
                        dev.azora.sdk.docking.presentation.NativeFloatingWindowHost(
                            layout = dockState.layout,
                            dragState = dockState.dragState,
                            onAction = onDockAction,
                            mainWindowScreenOffsetX = screenOffsetX,
                            mainWindowScreenOffsetY = screenOffsetY,
                            mainWindowDensity = mainWindowDensity,
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }
        }
    }
}

/**
 * Theme button that opens a dropdown with System / Light / Dark options.
 * The icon reflects the currently-effective light/dark mode.
 */
@Composable
private fun ThemeMenuButton(
    themePreference: ThemePreference,
    isDarkMode: Boolean,
    onThemeChange: (ThemePreference) -> Unit,
    buttonSize: Dp,
    iconSize: Dp,
    tint: androidx.compose.ui.graphics.Color
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                painter = composeResourcePainter(
                    if (isDarkMode) AppRes.drawable.ic_light_mode else AppRes.drawable.ic_dark_mode
                ),
                contentDescription = "Theme",
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                ThemePreference.SYSTEM to "System",
                ThemePreference.LIGHT to "Light",
                ThemePreference.DARK to "Dark"
            ).forEach { (preference, label) ->
                DropdownMenuItem(
                    text = { Text((if (preference == themePreference) "✓  " else "      ") + label) },
                    onClick = {
                        onThemeChange(preference)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Run / Stop controls that launch the generated project and stream its output to the
 * Console panel. Hidden when the current template can't be run from Studio.
 */
@Composable
private fun ProjectRunControls(
    show: Boolean,
    targets: List<RunTarget>,
    selected: RunTarget?,
    onSelect: (RunTarget) -> Unit,
    onRefresh: () -> Unit,
    isRunning: Boolean,
    onRun: () -> Unit,
    onStop: () -> Unit,
    buttonSize: Dp,
    iconSize: Dp,
    tint: androidx.compose.ui.graphics.Color
) {
    if (!show) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onRefresh(); expanded = true }
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = selected?.label ?: "Select target",
                color = tint,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 170.dp)
            )
            Text("▾", color = tint, fontSize = 9.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (targets.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No targets found") },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                targets.forEach { target ->
                    DropdownMenuItem(
                        text = { Text((if (target.id == selected?.id) "✓  " else "      ") + target.label) },
                        onClick = { onSelect(target); expanded = false }
                    )
                }
            }
        }
    }
    IconButton(onClick = onRun, enabled = !isRunning && selected != null, modifier = Modifier.size(buttonSize)) {
        Icon(
            painter = composeResourcePainter(AppRes.drawable.ic_play_arrow),
            contentDescription = "Run",
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
    IconButton(onClick = onStop, enabled = isRunning, modifier = Modifier.size(buttonSize)) {
        Icon(
            painter = composeResourcePainter(AppRes.drawable.ic_stop),
            contentDescription = "Stop",
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * macOS/Linux toolbar with theme toggle in the safe area.
 * Shows Build/Run/Stop buttons when the software_studio plugin is enabled.
 */
@Composable
private fun MacOSToolbar(
    title: String,
    isDarkMode: Boolean,
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    project: AzoraProjectModel? = null,
    pluginManager: dev.azora.sdk.plugin.presentation.PluginManager? = null,
    enabledPlugins: List<dev.azora.sdk.plugin.core.InstalledPlugin> = emptyList(),
    onPlay: () -> Unit = {},
    onStop: () -> Unit = {},
    runnable: Boolean = false,
    runTargets: List<RunTarget> = emptyList(),
    selectedTarget: RunTarget? = null,
    onSelectTarget: (RunTarget) -> Unit = {},
    onRefreshTargets: () -> Unit = {},
    isProjectRunning: Boolean = false,
    onRunProject: () -> Unit = {},
    onStopProject: () -> Unit = {}
) {
    val palette = LocalAzoraPalette.current
    val titleBarBackground = if (isDarkMode) AzoraPalette.Neutral90 else palette.surfaceTop
    val isSoftwareStudioEnabled = enabledPlugins.any { it.id == "dev.azora.software_studio" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(EDITOR_TOOLBAR_HEIGHT.dp)
            .background(titleBarBackground)
    ) {
        // Left side: undo/redo buttons
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    painter = composeResourcePainter(AppRes.drawable.ic_undo),
                    contentDescription = "Undo",
                    tint = if (canUndo) palette.contentTop.copy(alpha = 0.7f)
                           else palette.contentTop.copy(alpha = 0.2f),
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    painter = composeResourcePainter(AppRes.drawable.ic_redo),
                    contentDescription = "Redo",
                    tint = if (canRedo) palette.contentTop.copy(alpha = 0.7f)
                           else palette.contentTop.copy(alpha = 0.2f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Centered title
        Text(
            text = title,
            color = palette.contentTop,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center)
        )

        // Right side: app builder buttons + theme toggle
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Builder: Build / Run / Stop buttons
            if (isSoftwareStudioEnabled) {
                SoftwareStudioToolbarButtons(onPlay = onPlay, onStop = onStop)
            }

            // Run / Stop the generated project (output streams to the Console panel)
            ProjectRunControls(
                show = runnable,
                targets = runTargets,
                selected = selectedTarget,
                onSelect = onSelectTarget,
                onRefresh = onRefreshTargets,
                isRunning = isProjectRunning,
                onRun = onRunProject,
                onStop = onStopProject,
                buttonSize = 20.dp,
                iconSize = 14.dp,
                tint = palette.contentTop.copy(alpha = 0.7f)
            )

            // Theme selector (System / Light / Dark)
            ThemeMenuButton(
                themePreference = themePreference,
                isDarkMode = isDarkMode,
                onThemeChange = onThemeChange,
                buttonSize = 20.dp,
                iconSize = 14.dp,
                tint = palette.contentTop.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Custom title bar for the main editor window.
 * Matches the design of floating window title bars.
 * Shows Build/Run/Stop buttons when the software_studio plugin is enabled.
 */
@Composable
private fun MainWindowTitleBar(
    title: String,
    isDarkMode: Boolean,
    isMaximized: Boolean,
    isFullscreen: Boolean,
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onFullscreen: () -> Unit,
    onClose: () -> Unit,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    project: AzoraProjectModel? = null,
    pluginManager: dev.azora.sdk.plugin.presentation.PluginManager? = null,
    enabledPlugins: List<dev.azora.sdk.plugin.core.InstalledPlugin> = emptyList(),
    onPlay: () -> Unit = {},
    onStop: () -> Unit = {},
    runnable: Boolean = false,
    runTargets: List<RunTarget> = emptyList(),
    selectedTarget: RunTarget? = null,
    onSelectTarget: (RunTarget) -> Unit = {},
    onRefreshTargets: () -> Unit = {},
    isProjectRunning: Boolean = false,
    onRunProject: () -> Unit = {},
    onStopProject: () -> Unit = {}
) {
    val palette = LocalAzoraPalette.current
    val titleBarBackground = if (isDarkMode) AzoraPalette.Neutral90 else palette.surfaceTop
    val isSoftwareStudioEnabled = enabledPlugins.any { it.id == "dev.azora.software_studio" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(titleBarBackground)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo/Redo buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = composeResourcePainter(AppRes.drawable.ic_undo),
                    contentDescription = "Undo",
                    tint = if (canUndo) palette.contentTop.copy(alpha = 0.7f)
                           else palette.contentTop.copy(alpha = 0.2f),
                    modifier = Modifier.size(14.dp)
                )
            }
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = composeResourcePainter(AppRes.drawable.ic_redo),
                    contentDescription = "Redo",
                    tint = if (canRedo) palette.contentTop.copy(alpha = 0.7f)
                           else palette.contentTop.copy(alpha = 0.2f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Title
        Text(
            text = title,
            color = palette.contentTop,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // App Builder: Build / Run / Stop buttons
        if (isSoftwareStudioEnabled) {
            SoftwareStudioToolbarButtons(onPlay = onPlay, onStop = onStop)
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Window controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Run / Stop the generated project (output streams to the Console panel)
            ProjectRunControls(
                show = runnable,
                targets = runTargets,
                selected = selectedTarget,
                onSelect = onSelectTarget,
                onRefresh = onRefreshTargets,
                isRunning = isProjectRunning,
                onRun = onRunProject,
                onStop = onStopProject,
                buttonSize = 28.dp,
                iconSize = 14.dp,
                tint = palette.contentTop.copy(alpha = 0.5f)
            )

            // Theme selector (System / Light / Dark)
            ThemeMenuButton(
                themePreference = themePreference,
                isDarkMode = isDarkMode,
                onThemeChange = onThemeChange,
                buttonSize = 28.dp,
                iconSize = 14.dp,
                tint = palette.contentTop.copy(alpha = 0.5f)
            )

            // Minimize button
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = composeResourcePainter(AppRes.drawable.ic_minimize),
                    contentDescription = "Minimize",
                    tint = palette.contentTop.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Maximize/Restore button
            IconButton(
                onClick = onMaximize,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = composeResourcePainter(if (isMaximized) AppRes.drawable.ic_restore else AppRes.drawable.ic_maximize),
                    contentDescription = if (isMaximized) "Restore" else "Maximize",
                    tint = palette.contentTop.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Fullscreen button (like Mac green button)
            IconButton(
                onClick = onFullscreen,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = composeResourcePainter(if (isFullscreen) AppRes.drawable.ic_fullscreen_exit else AppRes.drawable.ic_fullscreen),
                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                    tint = palette.success.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = composeResourcePainter(AppRes.drawable.ic_close),
                    contentDescription = "Close",
                    tint = palette.contentTop.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Build / Run / Stop buttons rendered in the toolbar when the software_studio plugin is enabled.
 * Wired to AzoraNodes interpreter for script execution.
 */
@Composable
private fun SoftwareStudioToolbarButtons(
    onPlay: () -> Unit = {},
    onStop: () -> Unit = {}
) {
    val palette = LocalAzoraPalette.current
    val undoRedoCoordinator: GlobalUndoRedoCoordinator = koinInject()
    val azoraNodesViewModel: AzoraNodesViewModel = koinInject()
    val projectRepository: AzoraProjectRepository = koinInject()
    val nodesState by azoraNodesViewModel.state.collectAsState()
    val isRunning = nodesState.isRunning

    // Load global constants from project
    var globalConstants by remember { mutableStateOf(emptyList<dev.azora.sdk.core.project.domain.GlobalConstant>()) }
    LaunchedEffect(Unit) {
        val result = projectRepository.getProject()
        if (result is dev.azora.sdk.core.domain.util.Res.Success) {
            globalConstants = result.data.settings.globalConstants
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Build (validate + save)
        IconButton(
            onClick = {
                azoraNodesViewModel.onAction(dev.azora.studio.azora_nodes.AzoraNodesAction.Save)
                undoRedoCoordinator.clearAllHistory()
            },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                painter = composeResourcePainter(AppRes.drawable.ic_build),
                contentDescription = "Build",
                tint = palette.contentTop.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
        }

        // Run
        IconButton(
            onClick = {
                if (!isRunning) {
                    azoraNodesViewModel.run(globalConstants)
                    onPlay()
                }
            },
            enabled = !isRunning,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                painter = composeResourcePainter(AppRes.drawable.ic_play_arrow),
                contentDescription = "Run",
                tint = if (!isRunning) palette.success else palette.contentTop.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }

        // Stop
        IconButton(
            onClick = {
                if (isRunning) {
                    azoraNodesViewModel.stop()
                    onStop()
                }
            },
            enabled = isRunning,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                painter = composeResourcePainter(AppRes.drawable.ic_stop),
                contentDescription = "Stop",
                tint = if (isRunning) palette.error else palette.contentTop.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * About dialog showing application information.
 */
@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    safeAreaTop: Dp = 0.dp
) {
    val palette = LocalAzoraPalette.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(top = safeAreaTop + 32.dp, start = 32.dp, end = 32.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App icon
            androidx.compose.foundation.Image(
                painter = painterResource("azora_icon.png"),
                contentDescription = "Azora Icon",
                modifier = Modifier.size(80.dp)
            )

            // App name
            Text(
                text = "Azora Studio",
                color = palette.contentTop,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // Version
            Text(
                text = "Version ${BuildConfig.STUDIO_VERSION}",
                color = palette.contentMid,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = "A Kotlin Multiplatform game engine\nand visual editor",
                color = palette.contentMid,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Copyright
            Text(
                text = "\u00A9 2026 DoubleGArts",
                color = palette.contentLow,
                fontSize = 12.sp
            )
        }
    }
}
