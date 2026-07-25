package com.sylvester.rustsensei.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sylvester.rustsensei.R

/**
 * The RustSensei chevron mark, used as the brand avatar in empty and status
 * states across the app. Matches the launcher icon and launch splash so every
 * surface reads as the same product (replaces the former Ferris emoji).
 */
@Composable
fun SenseiMark(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    Image(
        painter = painterResource(id = R.drawable.rustsensei_mark),
        contentDescription = null,
        modifier = modifier.size(size)
    )
}
