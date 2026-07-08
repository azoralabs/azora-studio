package dev.azora.studio.content_browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.palette.AzoraPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Notepad-style editor for a single text file opened from the Content Browser.
 *
 * The content is the single source of truth held by [OpenTextFilesManager]; this
 * panel binds the editor to that state and auto-saves shortly after typing
 * pauses (Ctrl/Cmd+S forces it). Zoom with Ctrl/Cmd+scroll. Rendered as a
 * `txt_<id>` dock tab.
 */
@Composable
fun TextFilePanel(
    panelId: String,
    projectPath: String
) {
    val manager: OpenTextFilesManager = koinInject()
    val openFiles by manager.openFiles.collectAsState()
    val scope = rememberCoroutineScope()
    val state = openFiles[panelId]

    if (state == null) {
        // Panel referenced by a restored layout whose file mapping wasn't restored.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AzoraPalette.Neutral90)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "This file is no longer open.\nReopen it from the Content Browser.",
                color = AzoraPalette.Neutral50,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    // Auto-save: persist shortly after typing pauses.
    LaunchedEffect(panelId, state.content, state.isDirty) {
        if (!state.isDirty) return@LaunchedEffect
        delay(600)
        manager.saveFile(panelId)
    }

    var fontSize by remember { mutableStateOf(13) }

    Column(modifier = Modifier.fillMaxSize().background(AzoraPalette.Neutral90)) {
        // Header: file name + save state
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AzoraPalette.Neutral80)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = state.fileName,
                color = AzoraPalette.Neutral20,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (state.isDirty) {
                Text("●", color = AzoraPalette.AccentYellow, fontSize = 10.sp)
                Text(
                    "Saving…",
                    color = AzoraPalette.Neutral40,
                    fontSize = 10.sp
                )
            } else {
                Text(
                    "Saved",
                    color = AzoraPalette.Neutral60,
                    fontSize = 10.sp
                )
            }
        }

        // Editor
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AzoraPalette.Neutral90)
                // Ctrl/Cmd + scroll zooms (macOS trackpad pinch arrives as this).
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type != PointerEventType.Scroll) continue
                            val mods = event.keyboardModifiers
                            if (!mods.isCtrlPressed && !mods.isMetaPressed) continue
                            val dy = event.changes.fold(0f) { acc, c -> acc + c.scrollDelta.y }
                            if (dy != 0f) {
                                fontSize = (fontSize + if (dy < 0f) 1 else -1).coerceIn(9, 28)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            BasicTextField(
                value = state.content,
                onValueChange = { manager.updateContent(panelId, it) },
                textStyle = TextStyle(
                    color = AzoraPalette.Neutral10,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize + 5).sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AzoraPalette.AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                        val meta = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                        when {
                            meta && keyEvent.key == Key.S -> {
                                scope.launch { manager.saveFile(panelId) }
                                true
                            }
                            meta && (keyEvent.key == Key.Equals || keyEvent.key == Key.Plus) ->
                                { fontSize = (fontSize + 1).coerceAtMost(28); true }
                            meta && keyEvent.key == Key.Minus ->
                                { fontSize = (fontSize - 1).coerceAtLeast(9); true }
                            meta && keyEvent.key == Key.Zero -> { fontSize = 13; true }
                            else -> false
                        }
                    }
            )
        }
    }
}
