package com.sylvester.rustsensei.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════
// SYLTECH AI SYSTEMS — canonical brand palette
// Source of truth: syltech-brand-sheet.svg ("Signal Orange on Ink", geometric).
// Do not add decorative colors. Signal + Amber are the affirmatives; the
// neutrals carry structure; Danger is the one sanctioned semantic extension.
// ═══════════════════════════════════════════════════════════════════════

// Brand accents
val SignalOrange = Color(0xFFFF5C00)  // Primary accent — CTAs, emphasis, active state
val SignalBright = Color(0xFFFF7A2E)  // Pressed / hover
val SignalDeep   = Color(0xFF8A3200)  // Container / pressed background
val Amber        = Color(0xFFFFA566)  // Node / warm accent — in-progress, live marker

// Neutrals (Ink -> Slate ladder)
val Ink       = Color(0xFF0B0B0C)  // Background
val InkRaised = Color(0xFF111114)  // Raised background
val Slate     = Color(0xFF161619)  // Surface
val SlateHigh = Color(0xFF1C1C20)  // Surface container
val Panel     = Color(0xFF26262B)  // Panel / high container
val PanelHigh = Color(0xFF2A2A2F)  // Highest container
val Line      = Color(0xFF2A2A2F)  // Borders, dividers

// Text
val Mist = Color(0xFFCFCFD4)  // Primary text on dark
val Ash  = Color(0xFF8F8F97)  // Secondary text

// Semantic — Signal/Amber are affirmatives; Danger is the only sanctioned
// extension (correctness/error legibility), never used decoratively.
val Positive = SignalOrange  // Correct, complete
val Active   = Amber         // In-progress
val Danger   = Color(0xFFC8503C)  // Incorrect, error
val Neutral  = Ash

// ── Legacy names -> Syltech tokens (documented remap; keeps existing refs compiling) ──
val RustOrange       = SignalOrange
val NeonOrangeBright = SignalBright
val RustOrangeDark   = SignalDeep
val NeonCyan         = Amber          // cyan retired -> amber node
val WarningAmber     = Amber
val SuccessGreen     = Positive       // green retired -> Signal Orange affirmative
val ErrorNeon        = Danger
val CrispWhite       = Mist
val SecondaryText    = Ash
val OutlineDark      = Line
val DangerRed        = Danger
val MediumGray       = Ash

// Dark surface ladder (names consumed by Theme.kt) -> Ink -> Slate
val DarkBackground              = Ink
val DarkSurface                 = InkRaised
val DarkSurfaceVariant          = Slate
val DarkSurfaceContainer        = SlateHigh
val DarkSurfaceContainerHigh    = Panel
val DarkSurfaceContainerHighest = PanelHigh

// Light palette — dark-first stays the default; light is a disciplined
// Syltech derivation (warm paper neutrals + Signal Orange, Ink text).
val LightBackground     = Color(0xFFF4F2EE)
val LightSurface        = Color(0xFFFBFAF7)
val LightSurfaceVariant = Color(0xFFEDEBE6)

// Difficulty / status
val DifficultyBeginner     = Positive
val DifficultyIntermediate = Active
val DifficultyAdvanced     = Danger

// Code rendering
val InlineCodeBackground = SlateHigh
val InlineCodeText       = Amber
val CodeBlockBackground  = Ink

// Completion — distinct: complete = orange, in-progress = amber, not-started = ash
val CompletedBadge  = Positive
val InProgressBadge = Active
val NotStartedBadge = Ash

// Syntax highlighting — a functional editor theme (code semantics, not UI chrome);
// kept as a considered multi-hue scheme, anchored warm toward the brand.
val SyntaxKeyword    = Color(0xFFFF7B72)
val SyntaxString     = Color(0xFFE8975A)
val SyntaxType       = Color(0xFF79C0FF)
val SyntaxFunction   = Color(0xFFD2A8FF)
val SyntaxComment    = Color(0xFF6E7681)
val SyntaxNumber     = Color(0xFF7EE787)
val SyntaxMacro      = Color(0xFFFFA657)
val SyntaxLifetime   = Color(0xFFF778BA)
val SyntaxAttribute  = Color(0xFFA5D6FF)
val SyntaxOperator   = Mist
val SyntaxLineNumber = Color(0xFF3B4148)

// Glow — restrained (Syltech is flatter than the old neon look)
val PrimaryGlow = Color(0x33FF5C00)
val CyanGlow    = Color(0x26FFA566)
val SuccessGlow = Color(0x33FF5C00)
val ErrorGlow   = Color(0x26C8503C)
