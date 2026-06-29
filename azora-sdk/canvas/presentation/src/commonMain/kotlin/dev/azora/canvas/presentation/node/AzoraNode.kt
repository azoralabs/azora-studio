package dev.azora.canvas.presentation.node

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import dev.azora.canvas.domain.model.node.AzoraNodeModel
import dev.azora.canvas.domain.type.AzoraPortType
import dev.azora.sdk.core.theme.LocalAzoraPalette
import dev.azora.sdk.core.theme.palette.AzoraPalette
import kotlin.math.roundToInt

/**
 * Declarative description of a node's single input port.
 *
 * @property label Text shown next to the port in [AzoraInputPortRow]. The default is a non-empty
 *   space so the row keeps consistent height even when the host doesn't want a visible label.
 * @property type Drives the port shape and color.
 */
data class AzoraInputPortDef(
    val label: String = " ",
    val type: AzoraPortType
)

/**
 * Declarative description of one of a node's output ports.
 *
 * @property index Zero-based index used to correlate with persisted exec-link source indices and
 *   with [connectedOutputPortIndices] passed to [AzoraNode].
 * @property label Text shown next to the port.
 * @property type Drives the port shape and color.
 * @property enabled When false, the port is rendered greyed-out and ignores clicks. Useful for
 *   conditionally-available branches (e.g. `MATCH` cases that aren't configured yet).
 */
data class AzoraOutputPortDef(
    val index: Int,
    val label: String,
    val type: AzoraPortType,
    val enabled: Boolean = true
)

/**
 * Unreal-Blueprint-style node body with ports rendered inline next to the header content.
 *
 * The composable handles common node concerns - positioning, selection / link-source border,
 * left-click select, drag, right-click consume - and delegates the actual interior layout to
 * [headerContent]. The host wires ports into its layout via the two `@Composable () -> Unit`
 * callbacks that headerContent receives, which lets the same `AzoraNode` shell back many node
 * shapes (screen, if, root, ...).
 *
 * @param node Domain model providing position, width, etc.
 * @param isSelected Drives the selection border.
 * @param isLinkSource True while the user is dragging a new link out of this node.
 * @param linkTransitionType Type of the in-flight link, used to tint this node's border when it is
 *   a candidate target.
 * @param panOffset Current canvas pan, applied to the node's screen position.
 * @param onSelect Called on left-click.
 * @param onStartLink Called when the user starts a link drag from one of this node's outputs.
 * @param onEndLink Called when an in-flight link is dropped onto this node's input.
 * @param onMove Called per drag frame with the position delta.
 * @param onEndDrag Called once when the drag ends.
 * @param onDismissContextMenus Called when the user clicks or right-clicks on the node so the
 *   canvas can close any menus it had open elsewhere.
 * @param nodeColor Optional override for the node body / header tint.
 * @param borderColor Default (unselected, non-link-source) border color.
 * @param nodeWidth Optional fixed width; when null the node sizes itself to content within
 *   `120..480.dp`.
 * @param showBorder Set false to render a borderless body.
 * @param inputPortDef Optional input port definition; null produces a node with no input.
 * @param outputPortDefs Output port definitions in display order.
 * @param isInputConnected Whether the input port has at least one inbound link.
 * @param connectedOutputPortIndices Output indices that have at least one outbound link; used to
 *   draw the "filled" port style.
 * @param onInputPortPositioned Reports the input port's center in **root** coordinates so the
 *   canvas can anchor links to it.
 * @param onOutputPortPositioned Reports each output port's center in **root** coordinates,
 *   keyed by [AzoraOutputPortDef.index].
 * @param onPortContextMenu When non-null, a secondary click on an output port invokes this with the
 *   port's index and the click position local to the port. Hosts use it to show a per-port menu.
 * @param canAddOutputPort When true, renders an `+` add-slot row after the output ports (Unreal
 *   array-pin style). The host receives the click via [onAddOutputPort].
 * @param onAddOutputPort Invoked when the `+` add-slot row is clicked; only shown when
 *   [canAddOutputPort] is true.
 * @param headerContent Slot for the node's interior. Receives two `@Composable () -> Unit` slots
 *   (`inputPorts`, `outputPorts`) that the host places in its own layout - typically using
 *   [InputPortsWrapper] and [OutputPortsWrapper].
 */
@Composable
fun AzoraNode(
    node: AzoraNodeModel,
    isSelected: Boolean,
    isLinkSource: Boolean,
    linkTransitionType: AzoraPortType?,
    panOffset: Offset,
    onSelect: () -> Unit,
    onStartLink: (portType: AzoraPortType, outputPortIndex: Int) -> Unit,
    onEndLink: () -> Unit,
    onMove: (Offset) -> Unit,
    onEndDrag: () -> Unit = {},
    onDismissContextMenus: () -> Unit = {},
    nodeColor: Color? = null,
    borderColor: Color = LocalAzoraPalette.current.surfaceLow,
    nodeWidth: Dp? = null,
    showBorder: Boolean = true,
    inputPortDef: AzoraInputPortDef? = null,
    outputPortDefs: List<AzoraOutputPortDef> = emptyList(),
    isInputConnected: Boolean = false,
    connectedOutputPortIndices: Set<Int> = emptySet(),
    onInputPortPositioned: ((Offset) -> Unit)? = null,
    onOutputPortPositioned: ((index: Int, position: Offset) -> Unit)? = null,
    onPortContextMenu: ((portIndex: Int, rootPosition: Offset) -> Unit)? = null,
    onContextMenu: ((rootPosition: Offset) -> Unit)? = null,
    canAddOutputPort: Boolean = false,
    onAddOutputPort: (() -> Unit)? = null,
    headerContent: @Composable (
        inputPorts: @Composable () -> Unit,
        outputPorts: @Composable () -> Unit
    ) -> Unit
) {
    val linkSourceColor = nodeColor ?: when (linkTransitionType) {
        AzoraPortType.NAV_PUSH -> AzoraPalette.AccentGreen
        AzoraPortType.NAV_REPLACE -> AzoraPalette.AccentOrange
        AzoraPortType.NAV_DIALOG -> AzoraPalette.White
        else -> AzoraPalette.Transparent
    }

    val selectionColor = nodeColor ?: AzoraPalette.AccentBlue

    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnEndDrag by rememberUpdatedState(onEndDrag)
    val currentOnDismissContextMenus by rememberUpdatedState(onDismissContextMenus)

    val currentOnStartLink by rememberUpdatedState(onStartLink)
    val currentOnEndLink by rememberUpdatedState(onEndLink)

    val currentOnPortContextMenu by rememberUpdatedState(onPortContextMenu)
    val currentOnContextMenu by rememberUpdatedState(onContextMenu)
    val currentCanAddOutputPort by rememberUpdatedState(canAddOutputPort)
    val currentOnAddOutputPort by rememberUpdatedState(onAddOutputPort)

    // The body's position in root coordinates, so a right-click can be reported in root space to the
    // canvas overlay (which converts it back to canvas-local).
    var bodyPositionInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (node.positionX + panOffset.x).roundToInt(),
                    (node.positionY + panOffset.y).roundToInt()
                )
            }
    ) {
        // Single node body containing everything
        Column(
            modifier = Modifier
                .then(
                    if (nodeWidth != null) Modifier.width(nodeWidth)
                    else Modifier.widthIn(min = 120.dp, max = 480.dp).width(IntrinsicSize.Max)
                )
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (showBorder) {
                        Modifier.border(
                            width = if (isSelected || isLinkSource) 2.dp else 1.dp,
                            color = when {
                                isLinkSource -> linkSourceColor
                                isSelected -> selectionColor
                                else -> borderColor
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                )
                .background(LocalAzoraPalette.current.surfaceTop.copy(alpha = 0.8f))
                .onGloballyPositioned { bodyPositionInRoot = it.positionInRoot() }
                // Right-click: open the node's context menu (root coords) when the host supports it;
                // otherwise fall back to closing any open menu. Skips when a child (e.g. an output port)
                // already consumed the click, so a port right-click opens the port menu, not this one.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                val change = event.changes.firstOrNull() ?: continue
                                if (change.isConsumed) continue
                                change.consume()
                                val handler = currentOnContextMenu
                                if (handler != null) {
                                    handler(Offset(bodyPositionInRoot.x + change.position.x, bodyPositionInRoot.y + change.position.y))
                                } else {
                                    currentOnDismissContextMenus()
                                }
                            }
                        }
                    }
                }
                // Left-click and drag handler
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (down.isConsumed) return@awaitEachGesture

                        down.consume()
                        currentOnDismissContextMenus()
                        currentOnSelect()

                        var lastPosition = down.position
                        var hasDragged = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val dragAmount = change.position - lastPosition
                            lastPosition = change.position
                            if (dragAmount != Offset.Zero) {
                                hasDragged = true
                                change.consume()
                                currentOnMove(dragAmount)
                            }
                        }
                        if (hasDragged) {
                            currentOnEndDrag()
                        }
                    }
                }
        ) {
            // Create port composables with position reporting
            val currentOnInputPortPositioned by rememberUpdatedState(onInputPortPositioned)
            val currentOnOutputPortPositioned by rememberUpdatedState(onOutputPortPositioned)

            val inputPortsContent: @Composable () -> Unit = {
                if (inputPortDef != null) {
                    AzoraInputPortRow(
                        label = inputPortDef.label,
                        type = inputPortDef.type,
                        isConnected = isInputConnected,
                        notConnectedCenterColor = nodeColor ?: AzoraPalette.Black,
                        onPositioned = currentOnInputPortPositioned,
                        onClick = currentOnEndLink
                    )
                }
            }

            val outputPortsContent: @Composable () -> Unit = {
                outputPortDefs.forEach { port ->
                    AzoraOutputPortRow(
                        label = port.label,
                        type = port.type,
                        isConnected = port.index in connectedOutputPortIndices,
                        notConnectedCenterColor = nodeColor ?: AzoraPalette.Black,
                        enabled = port.enabled,
                        onPositioned = { position: Offset ->
                            currentOnOutputPortPositioned?.invoke(port.index, position)
                        },
                        onContextMenu = currentOnPortContextMenu?.let { handler ->
                            { localClick: Offset -> handler(port.index, localClick) }
                        },
                        onClick = { currentOnStartLink(port.type, port.index) }
                    )
                }
                if (currentCanAddOutputPort) {
                    AzoraAddOutputPortRow(onClick = { currentOnAddOutputPort?.invoke() })
                }
            }

            // Ports are rendered inside headerContent
            headerContent(inputPortsContent, outputPortsContent)
        }
    }
}

/**
 * The `+` add-slot row appended after a node's output ports (Unreal array-pin style). Sized to match
 * an [AzoraOutputPortRow] so the column stays aligned; clicking invokes [onClick].
 */
@Composable
private fun AzoraAddOutputPortRow(onClick: () -> Unit) {
    val palette = LocalAzoraPalette.current
    // A single `+` glyph in a port-sized (16.dp) box, aligned to the end like the port rows above it.
    Box(
        modifier = Modifier.clickable { onClick() }.padding(vertical = 2.dp).size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("+", color = palette.contentMid, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}