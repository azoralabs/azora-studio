package dev.azora.studio.az_script

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.theme.palette.AzoraPalette

/**
 * IntelliSense-style completion list rendered next to the caret.
 * Keyboard interaction (Up/Down/Enter/Tab/Esc) is handled by the editor.
 */
@Composable
internal fun AzCompletionPopup(
    items: List<AzCompletion>,
    selectedIndex: Int,
    onPick: (AzCompletion) -> Unit,
) {
    val scrollState = rememberScrollState()
    // Keep the selected row visible while navigating with the keyboard.
    LaunchedEffect(selectedIndex) {
        val rowPx = 26 * 2 // approx row height in px at typical density
        scrollState.animateScrollTo((selectedIndex * rowPx - 3 * rowPx).coerceAtLeast(0))
    }

    Column(
        modifier = Modifier
            .widthIn(min = 240.dp, max = 420.dp)
            .heightIn(max = 220.dp)
            .shadow(8.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(AzoraPalette.Neutral85)
            .border(1.dp, AzoraPalette.Neutral60, RoundedCornerShape(6.dp))
            .verticalScroll(scrollState)
            .padding(vertical = 2.dp)
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selected) AzoraPalette.AccentBlue.copy(alpha = 0.25f) else Color.Transparent)
                    .clickable { onPick(item) }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = kindGlyph(item.kind),
                    color = kindColor(item.kind),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(18.dp)
                )
                Text(
                    text = item.label,
                    color = AzoraPalette.Neutral10,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Spacer(Modifier.width(12.dp))
                if (item.detail.isNotBlank() && item.detail != item.label) {
                    Text(
                        text = item.detail,
                        color = AzoraPalette.Neutral50,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

private fun kindGlyph(kind: String): String = when (kind) {
    "function", "method" -> "ƒ"
    "variable", "param" -> "x"
    "field" -> "∙"
    "pack" -> "P"
    "enum" -> "E"
    "enumMember" -> "e"
    "keyword" -> "k"
    else -> "·"
}

private fun kindColor(kind: String): Color = when (kind) {
    "function", "method" -> AzoraPalette.AccentBlue
    "variable", "param" -> AzoraPalette.AccentCyan
    "field" -> AzoraPalette.AccentTeal
    "pack" -> AzoraPalette.AccentYellow
    "enum", "enumMember" -> AzoraPalette.AccentOrange
    "keyword" -> AzoraPalette.AccentPurple
    else -> AzoraPalette.Neutral40
}
