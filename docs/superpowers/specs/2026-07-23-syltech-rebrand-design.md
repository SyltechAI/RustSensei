# RustSensei v1.4.0 — Syltech Rebrand (app pass)

**Date:** 2026-07-23
**Status:** Approved — implementing
**Decisions (from the user):**
1. RustSensei becomes a **Syltech AI Systems product**. Repo transfer to the `SyltechAI` org is the agreed next logistics step (done deliberately, not in this pass).
2. **Keep** `applicationId com.sylvester.rustsensei` — preserves the live Play listing, installs, reviews. Visible identity becomes "RustSensei by Syltech".
3. This pass = **app rebrand only**, own PR + draft release (→ v1.4.0).

Design is the synthesized hybrid, grounded in the official brand kit
(`sylvester-francis/sylvester-francis/assets/brand/syltech-brand-sheet.svg`).
Reference artifact: the Syltech-hybrid Dashboard lookbook.

---

## 1. Brand tokens (verbatim from the brand sheet)

| Token | Name | Hex |
|---|---|---|
| Primary accent | Signal Orange | `#ff5c00` |
| Accent / node | Amber | `#ffa566` |
| Background | Ink | `#0b0b0c` |
| Background 2 | Slate | `#161619` |
| Text on dark | Mist | `#cfcfd4` |
| Secondary text | Ash | `#8f8f97` |
| Panel / border | (sheet) | `#26262b` / `#2a2a2f` |

Type: heavy geometric sans, uppercase + tracked — Helvetica/Arial Bold; web fallback **Archivo/Inter**; **monospace for code + figures**.
Voice: concrete, technical, understated. **No emoji, no em dashes, no marketing adjectives.**
Usage: **orange on ink by default** (dark-first). Geometric, flat.

**Sanctioned semantic extension** (learning app needs correct/incorrect legibility, absent from the brand sheet): `Danger #c8503c` (muted red), used ONLY for error/incorrect, never decoration. Correct/complete = Signal Orange; in-progress/active = Amber; neutral = Ash.

## 2. Strategy — one-file rebrand, then targeted components

Evidence: `RustOrange/NeonCyan/NeonOrangeBright/RustOrangeDark` have **0** references outside `Color.kt`/`Theme.kt`; 26 files use `AppColors.current.*`, 41 use `MaterialTheme.colorScheme.*` (both auto-rebrand centrally); 13 hardcoded hex remain in screens/components.

- Make **Color.kt** the canonical Syltech palette (correct names: `SignalOrange`, `Amber`, `Ink`, `Slate`, `Panel`, `Line`, `Mist`, `Ash`, `Danger`).
- Map every **legacy name** to a Syltech token explicitly (`val RustOrange = SignalOrange`, `val NeonCyan = Amber`, `val CrispWhite = Mist`, `val SecondaryText = Ash`, surface ladder → Ink→Slate). Documented aliases, not misleading raw values. Nothing outside Color.kt breaks; the whole app inherits Syltech color.

## 3. Workstreams

- **W1 Color** — rewrite `Color.kt` to the Syltech palette + documented legacy aliases; remap the dark surface ladder to Ink→Slate; derive a disciplined Syltech **light** palette (dark-first stays the default). Verify: build + full theme test.
- **W2 Type** — `Type.kt`: geometric-sans display/labels **uppercase + tracked**; bundle **JetBrains Mono** (OFL) for code/figures; bundle **Archivo** (OFL) for display/labels if the font binaries are obtainable, else system sans with the tracked treatment (documented). Label styles carry letter-spacing.
- **W3 Shapes** — `RustSenseiShapes`: **flat/geometric** small radii (2/4/6/8/12) replacing 4/8/12/16/28.
- **W4 Navigation** — rebuild the hand-built `RustSenseiNavigationBar` (`MainScreen.kt`): visible monoline icons + uppercase micro-labels, Signal Orange active + Amber node, hairline top border. Fixes the invisible-nav defect.
- **W5 Icon** — new Syltech-family adaptive icon: Ink tile, Signal Orange keyline, faint blueprint grid, a Rust glyph, one Amber node. Update `ic_launcher_foreground/background/monochrome` + the splash (v1.3.0) to Ink + the new mark.
- **W6 Stragglers + endorsement** — repoint the 13 hardcoded hex (widget, code blocks) to Syltech tokens; add "by Syltech / A Syltech AI System" endorsement in-app (header/about/splash); keep the launcher label "RustSensei".

## 4. Verification
Each workstream: `:app:assembleDebug` + `:app:testDebugUnitTest` green; `:app:lintRelease` clean at the end; signed `:app:bundleRelease`. Version `1.3.0 → 1.4.0` (versionCode 11 → 12).

## 5. Deferred (agreed, not this pass)
Repo transfer to the `SyltechAI` GitHub org; a RustSensei product page for `syltechai.dev`; per-screen spec-sheet header component rollout beyond the hero screens; syntax-palette harmonization (kept as a functional editor theme for now).
