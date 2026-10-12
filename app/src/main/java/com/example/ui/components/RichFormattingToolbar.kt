package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.FormatAction

@Composable
fun RichFormattingToolbar(
    onFormatClick: (FormatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarIconButton(
            icon = Icons.Default.FormatBold,
            contentDescription = "Bold",
            tag = "toolbar_bold",
            onClick = { onFormatClick(FormatAction.BOLD) }
        )
        ToolbarIconButton(
            icon = Icons.Default.FormatItalic,
            contentDescription = "Italic",
            tag = "toolbar_italic",
            onClick = { onFormatClick(FormatAction.ITALIC) }
        )
        ToolbarIconButton(
            icon = Icons.Default.FormatUnderlined,
            contentDescription = "Underline",
            tag = "toolbar_underline",
            onClick = { onFormatClick(FormatAction.UNDERLINE) }
        )
        ToolbarIconButton(
            icon = Icons.Default.FormatStrikethrough,
            contentDescription = "Strikethrough",
            tag = "toolbar_strike",
            onClick = { onFormatClick(FormatAction.STRIKETHROUGH) }
        )

        ToolbarDivider()

        ToolbarTextButton(
            text = "H1",
            contentDescription = "Heading 1",
            tag = "toolbar_h1",
            onClick = { onFormatClick(FormatAction.H1) }
        )
        ToolbarTextButton(
            text = "H2",
            contentDescription = "Heading 2",
            tag = "toolbar_h2",
            onClick = { onFormatClick(FormatAction.H2) }
        )
        ToolbarTextButton(
            text = "H3",
            contentDescription = "Heading 3",
            tag = "toolbar_h3",
            onClick = { onFormatClick(FormatAction.H3) }
        )

        ToolbarDivider()

        ToolbarIconButton(
            icon = Icons.Default.FormatListBulleted,
            contentDescription = "Bullet List",
            tag = "toolbar_bullets",
            onClick = { onFormatClick(FormatAction.BULLET_LIST) }
        )
        ToolbarIconButton(
            icon = Icons.Default.FormatListNumbered,
            contentDescription = "Numbered List",
            tag = "toolbar_numbered",
            onClick = { onFormatClick(FormatAction.NUMBERED_LIST) }
        )
        ToolbarIconButton(
            icon = Icons.Default.CheckBoxOutlineBlank,
            contentDescription = "Checkbox",
            tag = "toolbar_checkbox",
            onClick = { onFormatClick(FormatAction.CHECKBOX) }
        )
        ToolbarIconButton(
            icon = Icons.Default.FormatQuote,
            contentDescription = "Quote",
            tag = "toolbar_quote",
            onClick = { onFormatClick(FormatAction.QUOTE) }
        )
        ToolbarIconButton(
            icon = Icons.Default.HorizontalRule,
            contentDescription = "Divider",
            tag = "toolbar_divider",
            onClick = { onFormatClick(FormatAction.DIVIDER) }
        )
        ToolbarIconButton(
            icon = Icons.Default.FormatClear,
            contentDescription = "Clear Formatting",
            tag = "toolbar_clear",
            onClick = { onFormatClick(FormatAction.CLEAR) }
        )
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolbarTextButton(
    text: String,
    contentDescription: String,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}
