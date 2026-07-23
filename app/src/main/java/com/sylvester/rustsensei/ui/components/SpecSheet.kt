package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Syltech spec-sheet primitives. The brand reads as an engineering title block:
 * an uppercase, tracked, Signal Orange eyebrow over a heavy geometric title.
 */

/** Uppercase, tracked, Signal Orange eyebrow label. */
@Composable
fun SpecLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/** Left-aligned title block: an eyebrow over a heavy grotesk title. */
@Composable
fun SpecHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SpecLabel(eyebrow)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
