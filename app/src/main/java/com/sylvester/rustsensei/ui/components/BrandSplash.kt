package com.sylvester.rustsensei.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.sylvester.rustsensei.R
import kotlinx.coroutines.delay

/** Ink, matches the splash + brand background. */
private val SplashInk = Color(0xFF0B0B0C)

/**
 * Plays a full-screen Syltech brand splash on cold launch, then crossfades to
 * [content]. State is remembered per Activity composition, so it shows on a cold
 * start (Activity creation) and not on a warm resume from Recents.
 */
@Composable
fun AppWithSplash(
    splashMillis: Long = 1800L,
    content: @Composable () -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(splashMillis)
        showSplash = false
    }
    Crossfade(
        targetState = showSplash,
        animationSpec = tween(durationMillis = 350),
        label = "brand-splash"
    ) { splash ->
        if (splash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SplashInk),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.splash_brand),
                    contentDescription = "Syltech AI Systems",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            content()
        }
    }
}
