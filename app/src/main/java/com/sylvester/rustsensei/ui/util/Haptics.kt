package com.sylvester.rustsensei.ui.util

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Semantic haptic vocabulary.
 *
 * Keeps the *intent* of each haptic explicit at the call site instead of
 * scattering raw [HapticFeedbackType] constants — and replaces the previous
 * habit of using [HapticFeedbackType.LongPress] for every tap.
 */

/** Quiz grading: a correct answer feels like [HapticFeedbackType.Confirm], incorrect like [HapticFeedbackType.Reject]. */
fun HapticFeedback.answerFeedback(correct: Boolean) =
    performHapticFeedback(if (correct) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)

/** A switch toggled on/off. */
fun HapticFeedback.toggleFeedback(on: Boolean) =
    performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

/** A confirmatory action, such as sending a chat message. */
fun HapticFeedback.confirm() =
    performHapticFeedback(HapticFeedbackType.Confirm)
