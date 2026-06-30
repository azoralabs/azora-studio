package dev.azora.studio.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

@OptIn(ExperimentalComposeUiApi::class)
actual fun PointerEvent.isSecondaryClick(): Boolean = button == PointerButton.Secondary
