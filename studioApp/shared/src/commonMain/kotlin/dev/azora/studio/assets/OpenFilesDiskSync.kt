package dev.azora.studio.assets

import dev.azora.sdk.core.io.FileSystem
import dev.azora.sdk.core.io.ListResult
import dev.azora.sdk.docking.domain.DockAction
import dev.azora.sdk.docking.domain.DockStateManager
import dev.azora.studio.content_browser.OpenTextFilesManager
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps open editor tabs in sync with the filesystem: when a file backing an
 * open tab is renamed on disk the tab (and its manager state) follows the new
 * name; when it is deleted the tab closes.
 *
 * Detection is a poll (every [POLL_MS]) that diffs each watched directory's
 * listing against the previous tick: a tracked file that vanished while
 * exactly one untracked same-extension file appeared in its directory is a
 * rename; anything else that vanished was deleted (or moved away, which for a
 * tab is the same thing).
 *
 * [run] is safe to launch from multiple places — a mutex ensures a single
 * active loop; callers queue up as standbys and take over if the active one
 * is cancelled.
 */
class OpenFilesDiskSync(
    private val fileSystem: FileSystem,
    private val azScripts: OpenAzScriptFilesManager,
    private val texts: OpenTextFilesManager,
    private val nodes: OpenAzoraNodesFilesManager,
    private val scenes: OpenAzoraSceneFilesManager,
    private val tileMaps: OpenAzoraTileMapFilesManager,
    private val azscenes: OpenAzsceneFilesManager,
    private val dockStateManager: DockStateManager,
) {
    private val mutex = Mutex()

    suspend fun run() {
        mutex.withLock {
            val previousListings = mutableMapOf<String, Set<String>>()
            while (currentCoroutineContext().isActive) {
                runCatching { tick(previousListings) }
                delay(POLL_MS)
            }
        }
    }

    private class WatchedTab(
        val panelId: String,
        val path: String,
        /** Tab title for a given path; null = tab can't follow renames (close it instead). */
        val relocate: ((newPath: String) -> String)?,
        val close: () -> Unit,
    )

    private fun baseName(path: String) = path.substringAfterLast("/").substringBeforeLast(".")

    private fun watchedTabs(): List<WatchedTab> = buildList {
        azScripts.openFiles.value.forEach { (id, st) ->
            add(WatchedTab(id, st.filePath, { azScripts.relocate(id, it); baseName(it) }, { azScripts.closeFile(id) }))
        }
        texts.openFiles.value.forEach { (id, st) ->
            add(WatchedTab(id, st.filePath, { texts.relocate(id, it); it.substringAfterLast("/") }, { texts.closeFile(id) }))
        }
        nodes.openFiles.value.forEach { (id, st) ->
            add(WatchedTab(id, st.filePath, { nodes.relocate(id, it); baseName(it) }, { nodes.closeFile(id) }))
        }
        scenes.openFiles.value.forEach { (id, st) ->
            add(WatchedTab(id, st.filePath, { scenes.relocate(id, it); baseName(it) }, { scenes.closeFile(id) }))
        }
        tileMaps.openFiles.value.forEach { (id, st) ->
            add(WatchedTab(id, st.filePath, { tileMaps.relocate(id, it); baseName(it) }, { tileMaps.closeFile(id) }))
        }
        // Azscene panel ids encode the file path, so those tabs can't follow a
        // rename — they close like a delete.
        azscenes.openFiles.value.forEach { (id, st) ->
            add(WatchedTab(id, st.filePath, relocate = null, close = { azscenes.closeFile(id) }))
        }
    }

    private suspend fun tick(previousListings: MutableMap<String, Set<String>>) {
        val tabs = watchedTabs()
        if (tabs.isEmpty()) {
            previousListings.clear()
            return
        }

        val listings = mutableMapOf<String, Set<String>>()
        for (dir in tabs.map { it.path.substringBeforeLast("/") }.toSet()) {
            listings[dir] = when (val result = fileSystem.listDirectory(dir)) {
                is ListResult.Success -> result.files.filterNot { it.isDirectory }.map { it.name }.toSet()
                is ListResult.Error -> emptySet()
            }
        }

        val trackedPaths = tabs.map { it.path }.toSet()
        for (tab in tabs) {
            val dir = tab.path.substringBeforeLast("/")
            val name = tab.path.substringAfterLast("/")
            val now = listings[dir] ?: emptySet()
            if (name in now) continue

            val previous = previousListings[dir]
            val extension = name.substringAfterLast(".", "")
            val appeared = if (previous == null) emptyList() else
                (now - previous).filter {
                    it.substringAfterLast(".", "") == extension && "$dir/$it" !in trackedPaths
                }

            if (tab.relocate != null && appeared.size == 1) {
                val newPath = "$dir/${appeared.single()}"
                val newTitle = tab.relocate.invoke(newPath)
                dockStateManager.dispatch(DockAction.UpdatePanelTitle(tab.panelId, newTitle))
            } else {
                tab.close()
                dockStateManager.dispatch(DockAction.RemovePanel(tab.panelId))
            }
        }

        previousListings.clear()
        previousListings.putAll(listings)
    }

    private companion object {
        const val POLL_MS = 1500L
    }
}
