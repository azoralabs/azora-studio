package dev.azora.sdk.docking.presentation.renderer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import dev.azora.sdk.docking.domain.*
import dev.azora.sdk.docking.domain.DockDefaults.MAX_SPLIT_RATIO
import dev.azora.sdk.docking.domain.DockDefaults.MIN_SPLIT_RATIO
import dev.azora.sdk.docking.presentation.*
import dev.azora.sdk.docking.presentation.theme.LocalDockDimensions

/**
 * Renders a split node with two children and a draggable splitter.
 *
 * Lays out the two child nodes either horizontally (side by side) or
 * vertically (stacked) based on the split orientation. A [dev.azora.sdk.docking.presentation.DockSplitter]
 * between them allows the user to resize the split ratio.
 *
 * ## Size Calculation
 *
 * The available space (minus splitter) is divided according to the
 * split ratio:
 * - First child: `availableSize * ratio`
 * - Second child: `availableSize * (1 - ratio)`
 *
 * The ratio is clamped to [MIN_SPLIT_RATIO, MAX_SPLIT_RATIO] during
 * resize to prevent panels from becoming too small.
 *
 * @param node The split node to render
 * @param panelDescriptors Map of panel IDs to their descriptors
 * @param dragState Current drag operation state
 * @param maximizedPanelId ID of the maximized panel
 * @param containerSize Size of the container in pixels
 * @param callbacks Dock interaction callbacks
 *
 * @see dev.azora.sdk.docking.presentation.DockSplitter for the resize handle
 * @see DockNode.Split for the data model
 */
@Composable
internal fun RenderSplitNode(
    node: DockNode.Split,
    panelDescriptors: Map<String, DockPanelDescriptor>,
    dragState: DragState?,
    maximizedPanelId: String?,
    containerSize: IntSize,
    callbacks: DockCallbacks
) {
    val density = LocalDensity.current
    val dimensions = LocalDockDimensions.current

    // Use rememberUpdatedState to ensure callbacks always use the latest node ratio
    val currentNode by rememberUpdatedState(node)

    val splitterSizePx = with(density) { dimensions.splitterHitArea.toPx() }

    when (node.orientation) {
        DockOrientation.HORIZONTAL -> {
            Row(modifier = Modifier.fillMaxSize()) {
                val totalWidth = containerSize.width.toFloat()
                val availableWidth = totalWidth - splitterSizePx

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(density) { (availableWidth * node.ratio).toDp() })
                ) {
                    DockNodeRenderer(
                        node = node.first,
                        panelDescriptors = panelDescriptors,
                        dragState = dragState,
                        maximizedPanelId = maximizedPanelId,
                        callbacks = callbacks
                    )
                }

                DockSplitter(
                    orientation = DockOrientation.HORIZONTAL,
                    onDrag = { delta ->
                        val newRatio = (currentNode.ratio + delta / availableWidth).coerceIn(
                            MIN_SPLIT_RATIO,
                            MAX_SPLIT_RATIO
                        )
                        callbacks.onSplitResize(currentNode.id, newRatio)
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(density) { (availableWidth * (1 - node.ratio)).toDp() })
                ) {
                    DockNodeRenderer(
                        node = node.second,
                        panelDescriptors = panelDescriptors,
                        dragState = dragState,
                        maximizedPanelId = maximizedPanelId,
                        callbacks = callbacks
                    )
                }
            }
        }
        DockOrientation.VERTICAL -> {
            Column(modifier = Modifier.fillMaxSize()) {
                val totalHeight = containerSize.height.toFloat()
                val availableHeight = totalHeight - splitterSizePx

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { (availableHeight * node.ratio).toDp() })
                ) {
                    DockNodeRenderer(
                        node = node.first,
                        panelDescriptors = panelDescriptors,
                        dragState = dragState,
                        maximizedPanelId = maximizedPanelId,
                        callbacks = callbacks
                    )
                }

                DockSplitter(
                    orientation = DockOrientation.VERTICAL,
                    onDrag = { delta ->
                        val newRatio = (currentNode.ratio + delta / availableHeight).coerceIn(
                            MIN_SPLIT_RATIO,
                            MAX_SPLIT_RATIO
                        )
                        callbacks.onSplitResize(currentNode.id, newRatio)
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { (availableHeight * (1 - node.ratio)).toDp() })
                ) {
                    DockNodeRenderer(
                        node = node.second,
                        panelDescriptors = panelDescriptors,
                        dragState = dragState,
                        maximizedPanelId = maximizedPanelId,
                        callbacks = callbacks
                    )
                }
            }
        }
    }
}