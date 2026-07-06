package dev.azora.studio.az_script

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.palette.AzoraPalette
import dev.azora.studio.assets.OpenAzScriptFilesManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Code editor for a single `.az` (Azora language) file, rendered as an
 * `azs_<id>` dock tab.
 *
 * The content is the single source of truth held by [OpenAzScriptFilesManager];
 * this panel binds the editor to that state and saves back to disk on demand
 * (Save button or Ctrl/Cmd+S). Compilation/diagnostics integration is still
 * pending — for Azora Engine projects the code is compiled by the engine
 * toolchain when the project is run.
 */
@Composable
fun AzScriptFilePanel(panelId: String, projectPath: String) {
    val manager: OpenAzScriptFilesManager = koinInject()
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
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(AzoraPalette.Neutral90)) {
        // Header: file name + dirty indicator + Save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AzoraPalette.Neutral80)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${state.fileName}.az",
                color = AzoraPalette.Neutral20,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (state.isDirty) {
                Text("●", color = AzoraPalette.AccentYellow, fontSize = 10.sp)
                Text("Unsaved", color = AzoraPalette.Neutral40, fontSize = 10.sp)
            } else {
                Text("Saved", color = AzoraPalette.Neutral60, fontSize = 10.sp)
            }
            SaveButton(enabled = state.isDirty) {
                scope.launch { manager.saveFile(panelId) }
            }
        }

        // Editor
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AzoraPalette.Neutral90)
                .verticalScroll(scrollState)
                .padding(12.dp)
        ) {
            BasicTextField(
                value = state.sourceCode,
                onValueChange = { manager.updateSource(panelId, it) },
                textStyle = TextStyle(
                    color = AzoraPalette.Neutral10,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AzoraPalette.AccentBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown &&
                            keyEvent.key == Key.S &&
                            (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)
                        ) {
                            scope.launch { manager.saveFile(panelId) }
                            true
                        } else {
                            false
                        }
                    }
            )
        }
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) AzoraPalette.AccentBlue else AzoraPalette.Neutral70)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            "Save",
            color = if (enabled) AzoraPalette.Neutral90 else AzoraPalette.Neutral50,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
