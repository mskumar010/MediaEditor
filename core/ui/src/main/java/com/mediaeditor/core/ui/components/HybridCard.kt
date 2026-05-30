package com.mediaeditor.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun HybridCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val borderColor = MaterialTheme.colorScheme.outline

    val cardModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .border(1.dp, borderColor, shape)

    if (onClick != null) {
        Column(
            modifier = cardModifier
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            content = content
        )
    } else {
        Column(
            modifier = cardModifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            content = content
        )
    }
}
