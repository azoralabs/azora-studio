package dev.azora.canvas.presentation.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class NodeMenuItem(val label: String, val color: Color = Color.Unspecified, val onClick: () -> Unit)

data class NodeMenuSection(val header: String?, val items: List<NodeMenuItem>)

/**
 * Searchable context menu for node-graph canvases. A search field at the top filters items by label
 * (case-insensitive substring); auto-focuses on open; sections auto-expand while searching. Used by
 * any plugin that needs a right-click "add node / delete / duplicate" menu on the canvas.
 */
@Composable
fun NodeContextMenu(position: Offset, sections: List<NodeMenuSection>, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val filtered = remember(searchQuery, sections) {
        if (searchQuery.isBlank()) sections
        else sections.map { s -> s.copy(items = s.items.filter { it.label.contains(searchQuery, ignoreCase = true) }) }
            .filter { it.items.isNotEmpty() }
    }
    val totalItems = filtered.sumOf { it.items.size }

    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .width(IntrinsicSize.Max)
            .shadow(10.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(cs.surfaceContainer)
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(8.dp))
    ) {
        Column(Modifier.width(IntrinsicSize.Max).heightIn(max = 420.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = searchQuery, onValueChange = { searchQuery = it }, singleLine = true,
                    textStyle = TextStyle(color = cs.onSurface, fontSize = 13.sp),
                    cursorBrush = SolidColor(cs.primary),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) Text("Search…", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Text("×", color = cs.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { searchQuery = "" }.padding(start = 6.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(cs.outlineVariant))
            if (totalItems == 0) {
                Text("No results found", color = cs.onSurfaceVariant, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp))
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    filtered.forEachIndexed { sectionIndex, section ->
                        if (sectionIndex > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(cs.outlineVariant))
                        section.header?.let { header ->
                            Text(header, color = cs.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(cs.outlineVariant))
                        }
                        section.items.forEachIndexed { index, item ->
                            val textColor = if (item.color == Color.Unspecified) cs.onSurface else item.color
                            Box(Modifier.fillMaxWidth().clickable { item.onClick(); onDismiss() }.padding(horizontal = 14.dp, vertical = 9.dp)) {
                                Text(item.label, color = textColor, fontSize = 12.sp)
                            }
                            if (index < section.items.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(cs.outlineVariant.copy(alpha = 0.4f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NodeContextMenu(position: Offset, items: List<NodeMenuItem>, onDismiss: () -> Unit, header: String? = null) {
    NodeContextMenu(position, listOf(NodeMenuSection(header, items)), onDismiss)
}
