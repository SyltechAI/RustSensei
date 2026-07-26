@file:OptIn(ExperimentalTextApi::class)

package com.sylvester.rustsensei.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sylvester.rustsensei.R

// Syltech typography.
// Brand: heavy geometric sans, uppercase + tracked for labels (Helvetica/Arial
// Bold; web fallback Archivo/Inter). Android has no Helvetica, so the system
// grotesk (Roboto) stands in for pass 1 — bundling Archivo is a documented
// fast-follow. Monospace is reserved for code and figures (the spec-sheet
// readouts), applied at those call sites via CodeFontFamily.
val CodeFontFamily = FontFamily.Monospace

// Archivo variable font (Google Fonts, OFL-1.1), bundled at res/font/archivo.ttf.
// The brand's heavy geometric grotesk, now the real face instead of the Roboto stand-in.
private fun archivoAxes(weight: Int) =
    FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(100f))

internal val Archivo = FontFamily(
    Font(R.font.archivo, FontWeight.Normal, variationSettings = archivoAxes(400)),
    Font(R.font.archivo, FontWeight.Medium, variationSettings = archivoAxes(500)),
    Font(R.font.archivo, FontWeight.SemiBold, variationSettings = archivoAxes(600)),
    Font(R.font.archivo, FontWeight.Bold, variationSettings = archivoAxes(700)),
    Font(R.font.archivo, FontWeight.ExtraBold, variationSettings = archivoAxes(800)),
    Font(R.font.archivo, FontWeight.Black, variationSettings = archivoAxes(900))
)

private val Grotesk = Archivo

val RustSenseiTypography = Typography(
    // ── Display / headlines — heavy geometric grotesk, tight tracking ──
    headlineLarge = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    // ── Titles ──
    titleLarge = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    // ── Body ──
    bodyLarge = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    // ── Labels — wide-tracked for the spec-sheet uppercase eyebrows ──
    labelLarge = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Grotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.4.sp
    )
)
