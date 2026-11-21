package com.example.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = LocalBrutalPalette.current.surface,
    borderColor: Color = LocalBrutalPalette.current.border,
    shadowColor: Color = LocalBrutalPalette.current.shadow,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 4.dp,
    cornerRadius: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedShift by animateDpAsState(
        targetValue = if (onClick != null && isPressed) shadowOffset else 0.dp,
        animationSpec = spring(stiffness = 700f),
        label = "brutal_card_press",
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .drawBehind {
                val shiftPx = animatedShift.toPx()
                val offsetPx = shadowOffset.toPx()
                val rPx = cornerRadius.toPx()
                drawRoundRect(
                    color = shadowColor,
                    topLeft = Offset(offsetPx, offsetPx),
                    size = size,
                    cornerRadius = CornerRadius(rPx, rPx),
                )
                if (shiftPx > 0f) {
                    // Shadow stays anchored while foreground moves toward it
                }
            }
    ) {
        val fgModifier = Modifier
            .offset(x = animatedShift, y = animatedShift)
            .clip(shape)
            .background(containerColor, shape)
            .border(BorderStroke(borderWidth, borderColor), shape)
            .let { base ->
                if (onClick != null) {
                    base
                        .minimumInteractiveComponentSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = onClick,
                        )
                } else {
                    base
                }
            }
            .padding(contentPadding)

        Column(modifier = fgModifier, content = content)
    }
}

@Composable
fun BrutalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = LocalBrutalPalette.current.primaryAccent,
    contentColor: Color = LocalBrutalPalette.current.onPrimaryAccent,
    borderColor: Color = LocalBrutalPalette.current.border,
    shadowColor: Color = LocalBrutalPalette.current.shadow,
    shadowOffset: Dp = 4.dp,
    cornerRadius: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalBrutalPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shift by animateDpAsState(
        targetValue = if (enabled && isPressed) shadowOffset else 0.dp,
        animationSpec = spring(stiffness = 750f),
        label = "brutal_button_press",
    )
    val effectiveContainer = if (enabled) containerColor else palette.surfaceVariant
    val effectiveContent = if (enabled) contentColor else palette.textSecondary
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .drawBehind {
                val offsetPx = shadowOffset.toPx()
                val rPx = cornerRadius.toPx()
                drawRoundRect(
                    color = if (enabled) shadowColor else shadowColor.copy(alpha = 0.35f),
                    topLeft = Offset(offsetPx, offsetPx),
                    size = size,
                    cornerRadius = CornerRadius(rPx, rPx),
                )
            }
    ) {
        Row(
            modifier = Modifier
                .offset(x = shift, y = shift)
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(effectiveContainer, shape)
                .border(2.dp, borderColor, shape)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick,
                )
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides effectiveContent) {
                content()
            }
        }
    }
}

@Composable
fun BrutalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalBrutalPalette.current.surface,
    contentColor: Color = LocalBrutalPalette.current.textPrimary,
    shadowOffset: Dp = 3.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalBrutalPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shift by animateDpAsState(
        targetValue = if (isPressed) shadowOffset else 0.dp,
        animationSpec = spring(stiffness = 750f),
        label = "brutal_icon_press",
    )
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .padding(end = shadowOffset, bottom = shadowOffset)
            .drawBehind {
                val offsetPx = shadowOffset.toPx()
                val rPx = 8.dp.toPx()
                drawRoundRect(
                    color = palette.shadow,
                    topLeft = Offset(offsetPx, offsetPx),
                    size = size,
                    cornerRadius = CornerRadius(rPx, rPx),
                )
            }
    ) {
        Box(
            modifier = Modifier
                .offset(x = shift, y = shift)
                .size(48.dp)
                .clip(shape)
                .background(containerColor, shape)
                .border(2.dp, palette.border, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}

@Composable
fun BrutalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = LocalBrutalPalette.current.primaryAccent,
    selectedTextColor: Color = LocalBrutalPalette.current.onPrimaryAccent,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val palette = LocalBrutalPalette.current
    val bg = if (selected) selectedColor else palette.surface
    val fg = if (selected) selectedTextColor else palette.textPrimary
    val shape: Shape = RoundedCornerShape(8.dp)

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = shape,
        color = bg,
        border = BorderStroke(2.dp, palette.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                CompositionLocalProvider(LocalContentColor provides fg) {
                    leadingIcon()
                }
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BrutalTagBadge(
    text: String,
    containerColor: Color,
    contentColor: Color = LocalBrutalPalette.current.onPrimaryAccent,
    modifier: Modifier = Modifier,
) {
    val palette = LocalBrutalPalette.current
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor, shape)
            .border(1.5.dp, palette.border, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun BrutalProgressBar(
    progress: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
) {
    val palette = LocalBrutalPalette.current
    val clamped = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(palette.surfaceVariant, shape)
            .border(2.dp, palette.border, shape),
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .background(fillColor),
            )
        }
    }
}

@Composable
fun BrutalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    prefix: (@Composable () -> Unit)? = null,
) {
    val palette = LocalBrutalPalette.current
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textPrimary,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = palette.textSecondary) }
            } else null,
            singleLine = singleLine,
            maxLines = maxLines,
            textStyle = textStyle.copy(color = palette.textPrimary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.surface,
                focusedBorderColor = palette.border,
                unfocusedBorderColor = palette.border,
                cursorColor = palette.textPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
