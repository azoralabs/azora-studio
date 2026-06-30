package dev.azora.canvas.presentation.util

import androidx.compose.ui.geometry.Offset

/**
 * Default auto-layout: staggers [count] nodes in a grid (4 columns). Returns a map of node **index**
 * → [Offset] position. Plugins map `index → nodeId` at the call site.
 *
 * Used as the fallback when a node has no persisted position (e.g. just created or freshly loaded).
 */
fun autoLayout(
    count: Int,
    columns: Int = 4,
    startX: Float = 40f,
    startY: Float = 40f,
    columnStep: Float = 240f,
    rowStep: Float = 100f
): Map<Int, Offset> {
    val result = LinkedHashMap<Int, Offset>()
    for (i in 0 until count) {
        result[i] = Offset(startX + (i % columns) * columnStep, startY + (i / columns) * rowStep)
    }
    return result
}
