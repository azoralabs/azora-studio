package dev.azora.canvas.presentation.canvas

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.*
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * Foundation composable for the canvas — grid background, pan handling, and mouse tracking.
 *
 * Higher-level pieces ([AzoraEditorCanvas]) build on top of this. Responsibilities split as follows:
 * - **Visual chrome.** Draws an infinite-feeling grid that shifts with [panOffset], modulo the
 *   cell size, against a dark background.
 * - **Pan via right-click drag.** A right-button down is buffered; if the pointer moves, it is
 *   treated as a pan and reported via [onPanChange]; if the pointer is released without movement
 *   the click is forwarded to [onRightClick] so the host can show a context menu instead.
 * - **Left-click.** Forwarded as-is via [onLeftClick] for selection / clearing.
 * - **Mouse tracking.** The current pointer position (in this composable's local space) is passed
 *   into [content] so children can do hover and link-creation previews without re-installing
 *   pointer input.
 *
 * The composable keeps an internal mirror of [panOffset] so it can update mid-drag without
 * waiting for the host to round-trip the new value.
 *
 * @param panOffset Authoritative pan from state.
 * @param gridSize Distance between adjacent grid lines.
 * @param onPanChange Called per pan frame with the cumulative pan offset.
 * @param onEndPan Called once when a right-click drag ends.
 * @param onLeftClick Called on a left-click that wasn't consumed by a child.
 * @param onRightClick Called on a right-click that wasn't a pan; the [Offset] is in this
 *   canvas's local coordinate space.
 * @param modifier Layout modifier applied to the outer canvas box.
 * @param content Slot for everything drawn on top of the grid; receives the current mouse
 *   position in this canvas's local coordinates.
 */
@Composable
fun AzoraCanvas(
    panOffset: Offset,
    gridSize: Dp = 20.dp,
    onPanChange: (Offset) -> Unit,
    onEndPan: () -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(mousePosition: Offset) -> Unit
) {
    var localPanOffset by remember { mutableStateOf(panOffset) }
    var mousePosition by remember { mutableStateOf(Offset.Zero) }

    val currentOnLeftClick by rememberUpdatedState(onLeftClick)
    val currentOnRightClick by rememberUpdatedState(onRightClick)
    val currentOnEndPan by rememberUpdatedState(onEndPan)

    // Update local pan offset when external value changes
    if (localPanOffset != panOffset) {
        localPanOffset = panOffset
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AzoraPalette.Neutral90)
            .clipToBounds()
            // Track mouse position
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.type == PointerEventType.Move) {
                            event.changes.firstOrNull()?.let {
                                mousePosition = it.position
                            }
                        }
                    }
                }
            }
            // Left-click handler
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (event.type == PointerEventType.Press && event.buttons.isPrimaryPressed) {
                            val down = event.changes.firstOrNull() ?: continue
                            if (down.isConsumed) continue
                            down.consume()
                            currentOnLeftClick()
                        }
                    }
                }
            }
            // Right-click: show context menu or drag for panning
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val down = event.changes.firstOrNull() ?: continue
                            if (down.isConsumed) continue
                            down.consume()

                            val clickPosition = down.position
                            var lastPosition = down.position
                            var hasPanned = false

                            while (true) {
                                val dragEvent = awaitPointerEvent()
                                val change = dragEvent.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                val dragAmount = change.position - lastPosition
                                lastPosition = change.position
                                if (dragAmount != Offset.Zero) {
                                    hasPanned = true
                                    change.consume()
                                    localPanOffset += dragAmount
                                    onPanChange(localPanOffset)
                                }
                            }

                            if (hasPanned) {
                                currentOnEndPan()
                            } else {
                                currentOnRightClick(clickPosition)
                            }
                        }
                    }
                }
            }
    ) {
        // Draw grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSizePx = gridSize.toPx()
            val offsetX = localPanOffset.x % gridSizePx
            val offsetY = localPanOffset.y % gridSizePx

            var x = offsetX
            while (x < size.width) {
                drawLine(
                    color = AzoraPalette.Neutral80,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridSizePx
            }

            var y = offsetY
            while (y < size.height) {
                drawLine(
                    color = AzoraPalette.Neutral80,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridSizePx
            }
        }

        // Render content with mouse position
        content(mousePosition)
    }
}