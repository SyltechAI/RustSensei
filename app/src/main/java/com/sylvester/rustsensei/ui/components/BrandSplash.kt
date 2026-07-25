package com.sylvester.rustsensei.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.R
import kotlinx.coroutines.delay

private val Ink = Color(0xFF0B0B0C)
private val Signal = Color(0xFFFF5C00)
private val Mist = Color(0xFFCFCFD4)
private val Ash = Color(0xFF8F8F97)

/**
 * Full-screen RustSensei launch splash, drawn natively so it fills any screen
 * and stays crisp. The product (chevron mark + wordmark + tagline) leads;
 * Syltech AI Systems is the maker credit at the bottom. Shown on a cold launch,
 * then crossfades to [content]. State is remembered per Activity composition, so
 * it plays on a cold start and not on a warm resume from Recents.
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
        if (splash) BrandSplashContent() else content()
    }
}

@Composable
private fun BrandSplashContent() {
    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        // Product lockup, centered.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.rustsensei_mark),
                contentDescription = null,
                modifier = Modifier.size(104.dp)
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Signal)) { append("Rust") }
                    withStyle(SpanStyle(color = Mist)) { append("Sensei") }
                },
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(top = 22.dp)
            )
            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .width(48.dp)
                    .height(3.dp)
                    .background(Signal)
            )
            Text(
                text = "ON-DEVICE RUST TUTOR",
                color = Ash,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 14.dp)
            )
        }

        // Maker credit, quiet, at the bottom.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_mark),
                contentDescription = "Syltech AI Systems",
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SYLTECH AI SYSTEMS",
                color = Ash,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
        }
    }
}
