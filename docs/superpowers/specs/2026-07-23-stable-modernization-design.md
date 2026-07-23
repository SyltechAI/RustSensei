# RustSensei v1.3.0 — Stable UI/UX Modernization

**Date:** 2026-07-23
**Author:** Sylvester Francis (with Claude Code)
**Status:** Approved — implementing
**Source:** "Android UI/UX Modernization Research Report — July 2026" (senior principal engineer review)

---

## 1. Context

RustSensei is a shipping Play Store app (v1.2.4, versionCode 10): a Jetpack Compose
learning app with lessons, quizzes, code exercises, on-device LLM chat (LiteRT/Gemma),
a Rust playground, streaks, and a Glance widget. Single Gradle module (`:app`),
Compose BOM 2024.12.01, Kotlin 2.2.0, Hilt, targetSdk 35, minSdk 26.

A modernization research report recommended a broad shortlist. Before scoping, we
**validated the report** and **audited the codebase**. Findings drive this spec.

### 1.1 Claim validation (report is accurate)

All 8 load-bearing version/date claims were spot-checked live against `dl.google.com`
Maven metadata on 2026-07-23 and confirmed: compose-bom `2026.06.01` (stable),
material3 `1.4.0` (stable) / `1.5.0-alpha24` (prerelease), compose-bom-alpha `2026.07.00`,
core-splashscreen `1.2.0` (stable), Glance `1.1.1` (stable) + `1.2.0-rc01`/`1.3.0-alpha02`,
navigation3-runtime `1.1.4`, benchmark-macro-junit4 `1.4.1`, adaptive `1.2.0`/`1.3.0-rc01`.

**Critical catch:** `MotionScheme` / `MaterialTheme(motionScheme=…)` and the wavy
progress indicators are **1.5.0-alpha only** (motion scheme graduated in alpha15, wavy
in alpha18; 1.4.0-beta01 *removed* all Expressive APIs from the 1.4 line). So the
report's "wire motionScheme now / adopt wavy indicators" is **not achievable on the
stable BOM** — it requires the alpha channel.

### 1.2 Codebase audit — report premise is partly overstated

**Already done** (report lists as gaps): `enableEdgeToEdge()` (`MainActivity.kt:33`),
strong skipping (Kotlin 2.2.0), tonal surface-container roles (`Theme.kt:109-113,143-147`),
CompositionLocal design tokens (`Theme.kt:20-80`, `Tokens.kt`), monochrome themed icon
(`mipmap-anydpi-v26/ic_launcher.xml:5`), type-safe serializable nav routes, R8 full mode,
no portrait lock, ~35 existing animation call-sites (confetti, 3D flashcard flip, shimmer,
spring tab transitions), haptics present.

**Real gaps, stable-achievable:** BOM ~18 months behind; no splash-screen API; predictive
back not opted in; blunt haptics (every call `LongPress`); no Glance previews; **no
model-download notification**; no baseline-profile/macrobenchmark.

**Gaps requiring alpha (out of scope):** motionScheme wiring, MaterialExpressiveTheme,
wavy indicators, ButtonGroup/ToggleButton, flexible app bars, floating toolbar+FAB,
ShortNavigationBar/WideNavigationRail.

**Independent reliability finding:** the ~1.2 GB model download holds a
`PARTIAL_WAKE_LOCK` + `WifiLock` from `viewModelScope` (`ModelManager.kt:129-135`,
`ModelViewModel.kt:124`) with no foreground service/WorkManager — a direct hit on the
report's new wake-lock core vital (§7). 30-min timeout is the only safety net.

---

## 2. Scope decision

**Chosen: "Stable modernization" → v1.3.0**, with the **download reliability fix
included** as separated commits. No alpha Compose. This de-risks a later Expressive
flip while shipping real platform/reliability/polish wins now.

**Explicitly out of scope** (deferred to a future Expressive release on the alpha
channel): MaterialExpressiveTheme, motionScheme, wavy/ButtonGroup/ToggleButton/flexible
app bars/floating toolbar, Nav3 migration, adaptive/WindowSizeClass layouts, dynamic
color (deliberate brand decision, `Theme.kt:162` — not reopened here).

---

## 3. Workstreams

Each is an independently reviewable commit. W0 lands and goes green before W1–W6.

### W0 — Dependency upgrade (foundation)
- `compose-bom` 2024.12.01 → **2026.06.01** (material3 1.4.0, ui 1.11.4).
- `navigation-compose` 2.8.5 → **2.9.x** (needed for predictive-back NavHost animation).
- Bump companion androidx (`activity-compose`, `lifecycle-*`, `core-ktx`) **only as the
  build demands**; bump `compileSdk` 35 → 36 **only if forced** (install `android-36`).
- Address compile breakages from the jump (moved/deprecated APIs; frozen
  `material-icons-extended`).
- **Verify:** `:app:assembleDebug` compiles; `:app:testDebugUnitTest` fully green.

### W1 — Splash screen
- Add `androidx.core:core-splashscreen:1.2.0`. Add a `Theme.RustSensei.Starting`
  (windowSplashScreen background + themed `windowSplashScreenAnimatedIcon`). Call
  `installSplashScreen()` in `MainActivity.onCreate` before `super`/`setContent`.
- **Verify:** builds + lint; launches with splash.

### W2 — Predictive back
- Manifest: `android:enableOnBackInvokedCallback="true"` on `<application>`.
- nav 2.9.x provides default NavHost predictive-back animation. Existing `BackHandler`s
  left intact (still function); per-screen `PredictiveBackHandler` is future polish.
- **Verify:** builds; back gesture animates.

### W3 — Differentiated haptics
- Replace the single `HapticFeedbackType.LongPress` at all quiz/toggle sites with
  semantic types: `Confirm` (correct answer), `Reject` (incorrect) at `QuizScreen.kt:580,665`;
  `ToggleOn`/`ToggleOff` on settings toggles; keep sensible defaults elsewhere. (These
  constants are stable in ui 1.11.4.) Centralize via a small `HapticFeedback` helper.
- **Verify:** builds; review.

### W4 — Download reliability + notification *(separated commits)*
- Remove the manual `PARTIAL_WAKE_LOCK` from the download path (`ModelManager.kt:129-135`).
- Run the download under a **`dataSync` foreground service** (idiomatic for a
  user-initiated, progress-visible large download) so it survives backgrounding without an
  app-scoped wake lock. Rationale for choosing FGS over the report's literal "WorkManager":
  WorkManager targets deferrable/guaranteed background work; an active user-visible download
  is FGS territory. The requirement satisfied either way: **no manual wake lock.**
- Add a progress notification: `Notification.ProgressStyle` on Android 16+, classic
  `setProgress` fallback below.
- **Preserve** the existing `Flow<DownloadState>` progress API so the UI (`ModelSetupScreen`)
  is minimally touched.
- **Verify:** builds; existing download/model tests green; lifecycle reasoning documented.

### W5 — Glance widget previews
- `glance` 1.1.1 → **1.2.0-rc01** (only non-stable dep; rc, not alpha; report's "practical
  target"). Add `providePreview()` to `RustSenseiWidget`; register generated preview.
- **Verify:** builds + lint. (Revert to 1.1.1 without previews if zero-non-stable is required.)

### W6 — Baseline-profile infrastructure
- Add `androidx.profileinstaller:profileinstaller:1.4.1` to `:app` (safe, stable).
- Add a `:baselineprofile` module: `androidx.baselineprofile` plugin 1.4.1,
  `benchmark-macro-junit4:1.4.1`, a startup `BaselineProfileGenerator` + a
  `StartupBenchmark`.
- Profile **generation** requires a device/emulator: run here if one is reachable, else
  document `./gradlew :app:generateReleaseBaselineProfile` as a handoff step.
- **Verify:** module + `:app` build; benchmark run best-effort.

---

## 4. Verification strategy

- After W0 and again at the end: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- Final artifact: `./gradlew :app:bundleRelease` (signed; keystore present).
- Long builds run in background (fresh-dep download after an 18-month jump is large).
- Haptics/splash/predictive-back verified by build + code review (not unit-testable).

## 5. Release process

`1.2.4`→**`1.3.0`** (versionCode 10→11) in `app/build.gradle.kts` → push branch → open
**GitHub PR** → tag **`v1.3.0`** → GitHub release with notes + best-effort signed AAB.
**Play Console upload is the user's manual step** (no credentials; outward-facing).

## 6. Risks

1. **W0 breakage set is unpredictable** — 18-month jump; iterate against the compiler.
2. **compileSdk 36 likely** via companion libs → needs `sdkmanager` `android-36`.
3. **Gradle dependency download** needs network (sandbox may restrict) — test first.
4. **W4 touches core download code** — highest correctness risk; keep surgical, preserve API.
5. **Baseline profile gen needs a device** — may be a handoff step.
6. **Glance rc01** is non-stable — flagged; revertible.

## 7. Out of scope / future

Expressive release on compose-bom-alpha (MaterialExpressiveTheme, motionScheme, wavy,
ButtonGroup/ToggleButton, flexible app bars, floating toolbar), Nav3 + adaptive-navigation3,
WindowSizeClass/list-detail tablet layouts, dynamic color toggle, per-screen
PredictiveBackHandler, Roboto Flex / JetBrains Mono bundling.
