package dev.azora.studio.global_constants

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azora.sdk.core.project.domain.ConstantType
import dev.azora.sdk.core.project.domain.GlobalConstant
import dev.azora.sdk.core.theme.LocalAzoraPalette

/**
 * Main panel for managing global constants.
 */
@Composable
fun GlobalConstantsPanel(
    state: GlobalConstantsState,
    onAction: (GlobalConstantsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAzoraPalette.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surfaceMid)
    ) {
        // Header with title and add button
        GlobalConstantsHeader(onAction = onAction)

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.surfaceDisabled)
        )

        // Constants list
        if (state.constants.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No constants defined.\nClick \"+ Add Constant\" to create one.",
                    color = palette.contentLow,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.constants.forEach { constant ->
                    ConstantRow(
                        constant = constant,
                        isSelected = state.selectedId == constant.id,
                        onUpdate = { onAction(GlobalConstantsAction.Update(it)) },
                        onDelete = { onAction(GlobalConstantsAction.Remove(constant.id)) },
                        onSelect = { onAction(GlobalConstantsAction.Select(constant.id)) }
                    )
                }
            }
        }
    }
}

/**
 * Header with title and add constant dropdown.
 */
@Composable
private fun GlobalConstantsHeader(
    onAction: (GlobalConstantsAction) -> Unit
) {
    val palette = LocalAzoraPalette.current
    var showTypeMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Global Constants",
            color = palette.contentTop,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Box {
            AddConstantButton(
                onClick = { showTypeMenu = !showTypeMenu }
            )

            // Type selection dropdown
            if (showTypeMenu) {
                ConstantTypeDropdown(
                    onTypeSelected = { type ->
                        onAction(GlobalConstantsAction.Add(type))
                        showTypeMenu = false
                    },
                    onDismiss = { showTypeMenu = false }
                )
            }
        }
    }
}

/**
 * Add constant button.
 */
@Composable
private fun AddConstantButton(
    onClick: () -> Unit
) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        if (isHovered) palette.primary.copy(alpha = 0.8f) else palette.primary
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+ Add Constant",
            color = palette.content,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Dropdown menu for selecting constant type.
 */
@Composable
private fun ConstantTypeDropdown(
    onTypeSelected: (ConstantType) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalAzoraPalette.current

    androidx.compose.material3.DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(palette.surfaceTop)
    ) {
        ConstantType.entries.forEach { type ->
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypeBadge(type = type)
                        Text(
                            text = type.displayName,
                            color = palette.contentTop,
                            fontSize = 12.sp
                        )
                    }
                },
                onClick = { onTypeSelected(type) }
            )
        }
    }
}

/**
 * Row displaying a single constant with its editor.
 */
@Composable
private fun ConstantRow(
    constant: GlobalConstant,
    isSelected: Boolean,
    onUpdate: (GlobalConstant) -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        when {
            isSelected -> palette.surfaceTop
            isHovered -> palette.surfaceLow
            else -> palette.surfaceMid
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        // Top row: type badge, name, delete button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeBadge(type = constant.type)

            // Editable name field
            ConstantNameField(
                name = constant.name,
                onNameChange = { newName ->
                    onUpdate(constant.copy(name = newName))
                },
                modifier = Modifier.weight(1f)
            )

            // Delete button (visible on hover or selection)
            if (isHovered || isSelected) {
                DeleteButton(onClick = onDelete)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Value editor based on type
        when (constant.type) {
            ConstantType.BOOLEAN -> BooleanStudio(
                value = constant.value.toBooleanStrictOrNull() ?: false,
                onValueChange = { newValue ->
                    onUpdate(constant.copy(value = newValue.toString()))
                }
            )
            ConstantType.INTEGER -> NumberStudio(
                value = constant.value,
                onValueChange = { newValue ->
                    onUpdate(constant.copy(value = newValue))
                },
                isInteger = true
            )
            ConstantType.REAL -> NumberStudio(
                value = constant.value,
                onValueChange = { newValue ->
                    onUpdate(constant.copy(value = newValue))
                },
                isInteger = false
            )
            ConstantType.TEXT -> TextStudio(
                value = constant.value,
                onValueChange = { newValue ->
                    onUpdate(constant.copy(value = newValue))
                }
            )
        }
    }
}

/**
 * Type badge showing the constant type with color coding.
 */
@Composable
private fun TypeBadge(type: ConstantType) {
    val (label, color) = when (type) {
        ConstantType.BOOLEAN -> "B" to TypeColors.Boolean
        ConstantType.INTEGER -> "I" to TypeColors.Integer
        ConstantType.REAL -> "R" to TypeColors.Real
        ConstantType.TEXT -> "T" to TypeColors.Text
    }

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Editable name field for constant.
 */
@Composable
private fun ConstantNameField(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAzoraPalette.current
    var text by remember(name) { mutableStateOf(name) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    BasicTextField(
        value = text,
        onValueChange = { newValue ->
            // Only allow valid identifier characters
            val filtered = newValue.filter { it.isLetterOrDigit() || it == '_' }
            text = filtered
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (!focusState.isFocused && isFocused && text != name) {
                    onNameChange(text)
                }
                isFocused = focusState.isFocused
            },
        textStyle = TextStyle(
            color = palette.contentTop,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        ),
        singleLine = true,
        cursorBrush = SolidColor(palette.primary)
    )
}

/**
 * Delete button for removing a constant.
 */
@Composable
private fun DeleteButton(onClick: () -> Unit) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val color by animateColorAsState(
        if (isHovered) palette.error else palette.contentLow
    )

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u00D7",
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Toggle switch for boolean values.
 */
@Composable
private fun BooleanStudio(
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    val palette = LocalAzoraPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        when {
            value -> palette.primary
            isHovered -> palette.surfaceTop
            else -> palette.surfaceLow
        }
    )

    val indicatorColor by animateColorAsState(
        if (value) palette.content else palette.contentLow
    )

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable { onValueChange(!value) }
            .padding(2.dp),
        contentAlignment = if (value) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(indicatorColor)
        )
    }
}

/**
 * Number input field for integer and real values.
 */
@Composable
private fun NumberStudio(
    value: String,
    onValueChange: (String) -> Unit,
    isInteger: Boolean
) {
    val palette = LocalAzoraPalette.current
    var text by remember(value) { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }

    val isValid = if (isInteger) {
        text.toLongOrNull() != null || text.isEmpty() || text == "-"
    } else {
        text.toDoubleOrNull() != null || text.isEmpty() || text == "-" || text == "." || text == "-."
    }

    val borderColor by animateColorAsState(
        when {
            !isValid && text.isNotEmpty() -> palette.error
            isFocused -> palette.primary
            else -> palette.surfaceDisabled
        }
    )

    BasicTextField(
        value = text,
        onValueChange = { newValue ->
            // Allow valid number input characters
            val filtered = if (isInteger) {
                newValue.filter { it.isDigit() || it == '-' }
            } else {
                newValue.filter { it.isDigit() || it == '-' || it == '.' }
            }
            text = filtered
        },
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(palette.surfaceLow)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .onFocusChanged { focusState ->
                if (!focusState.isFocused && isFocused && text != value && isValid) {
                    onValueChange(text)
                }
                isFocused = focusState.isFocused
            },
        textStyle = TextStyle(
            color = palette.contentTop,
            fontSize = 13.sp
        ),
        singleLine = true,
        cursorBrush = SolidColor(palette.primary)
    )
}

/**
 * Text input field for string values.
 */
@Composable
private fun TextStudio(
    value: String,
    onValueChange: (String) -> Unit
) {
    val palette = LocalAzoraPalette.current
    var text by remember(value) { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        if (isFocused) palette.primary else palette.surfaceDisabled
    )

    BasicTextField(
        value = text,
        onValueChange = { newValue ->
            text = newValue
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(palette.surfaceLow)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .onFocusChanged { focusState ->
                if (!focusState.isFocused && isFocused && text != value) {
                    onValueChange(text)
                }
                isFocused = focusState.isFocused
            },
        textStyle = TextStyle(
            color = palette.contentTop,
            fontSize = 13.sp
        ),
        singleLine = true,
        cursorBrush = SolidColor(palette.primary)
    )
}

/**
 * Display name for constant types.
 */
private val ConstantType.displayName: String
    get() = when (this) {
        ConstantType.BOOLEAN -> "Boolean"
        ConstantType.INTEGER -> "Integer"
        ConstantType.REAL -> "Real"
        ConstantType.TEXT -> "Text"
    }

/**
 * Colors for type badges using Azora accent colors.
 */
private object TypeColors {
    val Boolean = dev.azora.sdk.core.theme.palette.AzoraPalette.AccentRed
    val Integer = dev.azora.sdk.core.theme.palette.AzoraPalette.AccentBlue
    val Real = dev.azora.sdk.core.theme.palette.AzoraPalette.AccentTeal
    val Text = dev.azora.sdk.core.theme.palette.AzoraPalette.AccentGreen
}
